package com.bytewise.game.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.ProgressState
import com.bytewise.game.data.model.Question
import com.bytewise.game.data.repository.SavedGame
import com.bytewise.game.domain.AnswerOutcome
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.domain.LevelRules
import com.bytewise.game.domain.NavigationRules
import com.bytewise.game.domain.QuestionSelector
import com.bytewise.game.domain.SarcasmGate
import com.bytewise.game.domain.ScoreCalculator
import com.bytewise.game.domain.ScoredAnswer
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteyReactions
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs one level: timer choice, question order, per-question clocks, scoring, streak/multiplier, navigation
 * between questions, answer feedback, Bytey events and state recovery.
 *
 * Timer rules
 *  - The player picks 20-90 s before the level starts (TIMER_SELECT phase); it applies to every question and never
 *    changes during the level.
 *  - Every question has its own clock ([remainingByIndex]). A clock only ever shrinks, by real elapsed time, while
 *    that question is on screen, unanswered and the game is not paused. Leaving a question (previous / next),
 *    opening the exit dialog or backgrounding the app freezes it; coming back restores exactly the stored value,
 *    never the full duration. An expired question stays expired (it is recorded as a timeout).
 *
 * Scoring rules
 *  - One record per question (unique index in Room). Points are never added incrementally: after every answer or
 *    answer change ALL points are recomputed from the final answers in question order ([ScoreCalculator.rescore]),
 *    so revisiting or changing answers can neither duplicate nor lose consistency.
 *  - Second-chance rule: a correct answer reached by CHANGING an earlier answer is worth a flat 5 points (no speed
 *    bonus, no x2) so "peek at the explanation, then switch" cannot be used to farm points.
 *  - A tap racing the timeout cannot be counted twice (state checks on the main thread + the unique DB index).
 */
class GameViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val container = (app as BytewiseApplication).container
    private val content = container.contentRepository
    private val sessions = container.sessionRepository
    private val savedGames = container.gameStateRepository
    private val progressRepo = container.progressRepository

    private val requestedLevel: Int = handle.get<Int>(GameArgs.LEVEL) ?: GameArgs.AUTO_LEVEL
    private val requestedMode: String = handle.get<String>(GameArgs.MODE) ?: GameArgs.MODE_AUTO

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _remainingMs = MutableStateFlow(GameConfig.QUESTION_TIME_MS)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    private val _events = MutableSharedFlow<ByteyEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ByteyEvent> = _events.asSharedFlow()

    private val _navigation = Channel<GameNav>(Channel.BUFFERED)
    val navigation: Flow<GameNav> = _navigation.receiveAsFlow()

    /** Serialises database/DataStore writes so "answer recorded" always precedes "advance" and "finish". */
    private val ioLock = Mutex()

    private var order: List<Question> = emptyList()
    private var pendingQuestions: List<Question> = emptyList()
    private var pendingLevel = 0
    private var sessionId = 0L
    private var level = 0
    private var timerSeconds = GameConfig.QUESTION_TIME_SECONDS
    private val durationMs: Long get() = timerSeconds * 1000L

    /** Final answer per question index (null = not answered yet). Resolved questions are always a prefix. */
    private var answers: Array<AnswerRecord?> = emptyArray()
    private var outcomes: List<AnswerOutcome> = emptyList()
    /** Time left on each question. Frozen while the question is not on screen. */
    private var remainingByIndex: LongArray = LongArray(0)

    private var index = 0
    private var score = 0
    private var correctCount = 0
    private var streak = 0
    private var remainingTimeMs = GameConfig.QUESTION_TIME_MS
    private var lastSyncAt = 0L
    private var answering = false
    private var lifecyclePaused = false
    private var tickJob: Job? = null
    private var bestBefore = 0
    private var unlockedBefore: Set<Int> = setOf(1)
    private var finishing = false
    private val quipGate = SarcasmGate(minGapMs = 25_000L)

    init {
        viewModelScope.launch { start() }
    }

    // ---------------------------------------------------------------- start / resume

    private suspend fun start() {
        val questions = content.bundle().questions
        if (questions.isEmpty()) return fail(GameError.NO_QUESTIONS)
        val progress = progressRepo.progress.first()
        unlockedBefore = progress.unlockedLevels
        if (requestedMode != GameArgs.MODE_NEW && tryResume(questions, progress)) return
        prepareNew(questions, progress)
    }

    private fun fail(error: GameError) {
        _state.update { it.copy(phase = GamePhase.ERROR, error = error) }
    }

    private suspend fun tryResume(allQuestions: List<Question>, progress: ProgressState): Boolean {
        val saved = savedGames.load() ?: return false
        val session = sessions.getSession(saved.sessionId)
        if (session == null || session.completedAt != null) {
            savedGames.clear()
            return false
        }
        if (requestedMode == GameArgs.MODE_RESUME && requestedLevel != session.level) return false
        val restored = QuestionSelector.restoreOrder(saved.questionIds, allQuestions) ?: return false

        order = restored
        sessionId = session.sessionId
        level = session.level
        timerSeconds = GameConfig.normalizeTimer(session.timerSeconds)
        bestBefore = progress.bestScores[level] ?: 0

        answers = arrayOfNulls(order.size)
        val byQuestion = order.withIndex().associate { (i, q) -> q.id to i }
        for (record in sessions.answersForSession(session.sessionId)) {
            val i = byQuestion[record.questionId] ?: continue
            answers[i] = record
        }
        // Resolved questions must be a prefix; anything after the first gap would be inconsistent, so drop it.
        val firstGap = answers.indexOfFirst { it == null }.let { if (it < 0) order.size else it }
        for (i in firstGap until answers.size) answers[i] = null

        remainingByIndex = LongArray(order.size) { i ->
            val record = answers[i]
            when {
                record != null && record.wasTimeout -> 0L
                record != null -> saved.remainingByIndex[i]
                    ?: (durationMs - (record.responseTimeSeconds * 1000f).toLong()).coerceAtLeast(0L)
                i == saved.index -> (saved.remainingByIndex[i] ?: saved.remainingMs).coerceIn(1L, durationMs)
                else -> (saved.remainingByIndex[i] ?: durationMs).coerceIn(1L, durationMs)
            }
        }
        rescoreAll()
        index = saved.index.coerceIn(0, minOf(firstGap, order.size - 1))
        showIndex(index)
        return true
    }

    /** Picks the level and shows the timer picker; the session is only created once the player confirms. */
    private fun prepareNew(allQuestions: List<Question>, progress: ProgressState) {
        val target = if (requestedLevel > 0) requestedLevel else nextLevelToPlay(progress)
        if (target !in progress.unlockedLevels) return fail(GameError.LOCKED)
        val levelQuestions = allQuestions.filter { it.level == target }
        if (levelQuestions.isEmpty()) return fail(GameError.NO_QUESTIONS)
        pendingLevel = target
        pendingQuestions = levelQuestions
        bestBefore = progress.bestScores[target] ?: 0
        _state.update {
            it.copy(
                phase = GamePhase.TIMER_SELECT,
                level = target,
                total = levelQuestions.size,
                pendingTimerSeconds = GameConfig.QUESTION_TIME_SECONDS,
                timerSeconds = GameConfig.QUESTION_TIME_SECONDS,
            )
        }
    }

    /** Lowest unlocked level that is not completed yet; when everything is completed, the highest level. */
    private fun nextLevelToPlay(progress: ProgressState): Int {
        val unlocked = progress.unlockedLevels.sorted()
        return unlocked.firstOrNull { it !in progress.completedLevels } ?: unlocked.last()
    }

    // ---------------------------------------------------------------- timer picker

    fun selectTimer(seconds: Int) {
        if (_state.value.phase != GamePhase.TIMER_SELECT || seconds !in GameConfig.TIMER_OPTIONS_SECONDS) return
        _state.update { it.copy(pendingTimerSeconds = seconds) }
    }

    /** Starts the level with the highlighted duration. Guarded so a double tap creates only one session. */
    fun confirmTimer() {
        val s = _state.value
        if (s.phase != GamePhase.TIMER_SELECT || sessionId != 0L || pendingQuestions.isEmpty()) return
        val chosen = GameConfig.normalizeTimer(s.pendingTimerSeconds)
        _state.update { it.copy(phase = GamePhase.LOADING) }
        viewModelScope.launch {
            level = pendingLevel
            timerSeconds = chosen
            order = QuestionSelector.buildOrder(pendingQuestions)
            sessionId = sessions.startSession(level, chosen)
            answers = arrayOfNulls(order.size)
            remainingByIndex = LongArray(order.size) { durationMs }
            outcomes = emptyList()
            index = 0
            rescoreAll()
            persist()
            showIndex(0)
        }
    }

    // ---------------------------------------------------------------- showing questions

    /** Shows question [i] with its own stored clock (never reset) and its stored answer, if any. */
    private fun showIndex(i: Int) {
        stopTicking(sync = false)
        index = i
        val record = answers[i]
        if (record != null) {
            answering = false
            remainingTimeMs = remainingByIndex[i]
        } else {
            answering = true
            remainingTimeMs = remainingByIndex[i].coerceIn(1L, durationMs)
        }
        _remainingMs.value = remainingTimeMs
        publishState(phase = GamePhase.PLAYING)
        if (answering && !isPaused()) startTicking(i)
    }

    private fun feedbackFor(i: Int): AnswerFeedback? {
        val record = answers[i] ?: return null
        return AnswerFeedback(
            selectedIndex = record.selectedIndex,
            isCorrect = record.isCorrect,
            timedOut = record.wasTimeout,
            earnedPoints = record.earnedPoints,
            multiplierApplied = outcomes.getOrNull(i)?.multiplierApplied == true,
        )
    }

    private fun resolvedCount(): Int = answers.count { it != null }

    private fun publishState(phase: GamePhase = _state.value.phase) {
        val record = answers[index]
        val feedback = feedbackFor(index)
        val statuses = answers.map { a ->
            when {
                a == null -> QuestionStatus.PENDING
                a.wasTimeout -> QuestionStatus.EXPIRED
                a.isCorrect -> QuestionStatus.CORRECT
                else -> QuestionStatus.WRONG
            }
        }
        val multiplier = if (record == null) {
            ScoreCalculator.isMultiplierActive(streak)
        } else {
            outcomes.getOrNull(index)?.multiplierActiveNext == true
        }
        _state.update {
            it.copy(
                phase = phase,
                error = null,
                level = level,
                question = order.getOrNull(index),
                index = index,
                total = order.size,
                score = score,
                correctCount = correctCount,
                streak = streak,
                multiplierActive = multiplier,
                feedback = feedback,
                timerSeconds = timerSeconds,
                canGoPrevious = NavigationRules.canGoPrevious(index),
                canGoNext = feedback != null,
                canChangeAnswer = NavigationRules.canChangeAnswer(
                    answered = record != null,
                    timedOut = record?.wasTimeout == true,
                    remainingMs = remainingByIndex.getOrElse(index) { 0L },
                ),
                statuses = statuses,
            )
        }
    }

    // ---------------------------------------------------------------- timer

    private fun isPaused(): Boolean = lifecyclePaused || _state.value.exitDialogVisible

    private fun startTicking(forIndex: Int) {
        stopTicking(sync = false)
        lastSyncAt = SystemClock.elapsedRealtime()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                syncRemaining()
                _remainingMs.value = remainingTimeMs
                if (remainingTimeMs <= 0L) {
                    onTimeout(forIndex)
                    break
                }
            }
        }
    }

    private fun stopTicking(sync: Boolean) {
        if (sync && tickJob?.isActive == true) syncRemaining()
        tickJob?.cancel()
        tickJob = null
    }

    private fun syncRemaining() {
        val now = SystemClock.elapsedRealtime()
        remainingTimeMs = (remainingTimeMs - (now - lastSyncAt)).coerceAtLeast(0L)
        lastSyncAt = now
    }

    /** Writes the on-screen question's running clock back into its slot (call after stopTicking(sync = true)). */
    private fun storeCurrentRemaining() {
        if (answering && index in remainingByIndex.indices) remainingByIndex[index] = remainingTimeMs
    }

    /** App went to the background / came back (driven by the screen's lifecycle events). */
    fun setLifecyclePaused(paused: Boolean) {
        if (lifecyclePaused == paused) return
        lifecyclePaused = paused
        applyPause()
    }

    fun showExitDialog() {
        if (!_state.value.exitDialogVisible) _events.tryEmit(ByteyEvent.EXIT_GAME)
        _state.update { it.copy(exitDialogVisible = true) }
        applyPause()
    }

    fun hideExitDialog() {
        _state.update { it.copy(exitDialogVisible = false) }
        applyPause()
    }

    private fun applyPause() {
        if (_state.value.phase != GamePhase.PLAYING) return
        if (isPaused()) {
            if (answering) {
                stopTicking(sync = true)
                storeCurrentRemaining()
                _remainingMs.value = remainingTimeMs
            }
            persist()
        } else if (answering && tickJob == null) {
            startTicking(index)
        }
    }

    // ---------------------------------------------------------------- answering / changing answers

    fun submitAnswer(selectedIndex: Int) {
        if (_state.value.phase != GamePhase.PLAYING || isPaused() || finishing) return
        if (selectedIndex !in 0 until GameConfig.OPTION_COUNT) return
        val question = order.getOrNull(index) ?: return
        val existing = answers[index]
        if (existing == null) {
            if (!answering) return
            answering = false
            stopTicking(sync = true)
            remainingByIndex[index] = remainingTimeMs
            _remainingMs.value = remainingTimeMs
            val seconds = (durationMs - remainingTimeMs) / 1000f
            registerFirstAnswer(question, selectedIndex, seconds, timedOut = false)
        } else {
            changeAnswer(question, existing, selectedIndex)
        }
    }

    private fun onTimeout(forIndex: Int) {
        if (!answering || index != forIndex || answers[forIndex] != null) return
        val question = order.getOrNull(forIndex) ?: return
        answering = false
        remainingTimeMs = 0L
        remainingByIndex[forIndex] = 0L
        _remainingMs.value = 0L
        registerFirstAnswer(question, -1, timerSeconds.toFloat(), timedOut = true)
    }

    private fun registerFirstAnswer(question: Question, selected: Int, seconds: Float, timedOut: Boolean) {
        val correct = !timedOut && selected == question.correctIndex
        answers[index] = AnswerRecord(
            sessionId = sessionId,
            questionId = question.id,
            conceptId = question.conceptId,
            level = question.level,
            selectedIndex = if (timedOut) -1 else selected,
            correctIndex = question.correctIndex,
            isCorrect = correct,
            responseTimeSeconds = seconds,
            earnedPoints = 0,
            wasTimeout = timedOut,
            answeredAt = System.currentTimeMillis(),
        )
        rescoreAll()
        publishState()
        val outcome = outcomes[index]
        _events.tryEmit(ByteyReactions.forAnswer(correct, timedOut, outcome.isFast, outcome.streakMilestone))
        persistAnswers()
        persist()
    }

    /** Replaces the stored answer of an already answered question (never adds a second one). */
    private fun changeAnswer(question: Question, existing: AnswerRecord, newSelected: Int) {
        if (!NavigationRules.canChangeAnswer(true, existing.wasTimeout, remainingByIndex[index])) return
        if (newSelected == existing.selectedIndex) return
        answers[index] = existing.copy(
            selectedIndex = newSelected,
            isCorrect = newSelected == question.correctIndex,
            changeCount = existing.changeCount + 1,
            answeredAt = System.currentTimeMillis(),
        )
        rescoreAll()
        publishState()
        if (quipGate.tryAcquire(SystemClock.elapsedRealtime())) {
            _events.tryEmit(ByteyEvent.ANSWER_CHANGED)
        } else {
            val o = outcomes[index]
            _events.tryEmit(ByteyReactions.forAnswer(o.isCorrect, false, false, false))
        }
        persistAnswers()
    }

    /** Recomputes points, correct count and streak from the final answers (single source of truth). */
    private fun rescoreAll() {
        val resolved = answers.takeWhile { it != null }.filterNotNull()
        outcomes = ScoreCalculator.rescore(
            resolved.map { ScoredAnswer(it.isCorrect, it.responseTimeSeconds, it.changeCount > 0) },
            timerSeconds.toFloat(),
        )
        resolved.forEachIndexed { i, record ->
            if (record.earnedPoints != outcomes[i].earnedPoints) answers[i] = record.copy(earnedPoints = outcomes[i].earnedPoints)
        }
        score = outcomes.sumOf { it.earnedPoints }
        correctCount = outcomes.count { it.isCorrect }
        streak = ScoreCalculator.currentStreak(outcomes.map { it.isCorrect })
    }

    private fun persistAnswers() {
        val snapshot = answers.filterNotNull()
        viewModelScope.launch { ioLock.withLock { sessions.saveAnswers(snapshot) } }
    }

    // ---------------------------------------------------------------- navigation / finish / leave

    fun previous() {
        if (_state.value.phase != GamePhase.PLAYING || finishing || isPaused()) return
        if (!NavigationRules.canGoPrevious(index)) return
        moveTo(index - 1)
        if (quipGate.tryAcquire(SystemClock.elapsedRealtime())) _events.tryEmit(ByteyEvent.NAV_BACK)
    }

    /** "Next question" / "See results". Only available once the current question is answered or expired. */
    fun next() {
        val s = _state.value
        if (s.phase != GamePhase.PLAYING || finishing || isPaused() || answers[index] == null) return
        if (index + 1 >= order.size) {
            if (NavigationRules.canFinish(index, order.size, resolvedCount())) finish()
            return
        }
        moveTo(index + 1)
    }

    /** Freezes the current clock, then shows [target] with its own stored clock. */
    private fun moveTo(target: Int) {
        if (answering) {
            stopTicking(sync = true)
            storeCurrentRemaining()
        } else {
            stopTicking(sync = false)
        }
        index = target
        persist()
        showIndex(target)
    }

    private fun finish() {
        if (finishing) return
        finishing = true
        stopTicking(sync = false)
        _state.update { it.copy(phase = GamePhase.FINISHING, feedback = null) }
        viewModelScope.launch {
            ioLock.withLock {
                val totalLevels = content.levelCount()
                val session = sessions.completeSession(sessionId, totalLevels)
                savedGames.clear()
                if (session == null) {
                    fail(GameError.SAVE_FAILED)
                    return@withLock
                }
                val next = if (session.passed) LevelRules.nextLevelToUnlock(session.level, totalLevels) else null
                val unlocked = if (next != null && next !in unlockedBefore) next else GameArgs.NO_UNLOCK
                val newBest = bestBefore > 0 && session.score > bestBefore
                _navigation.send(GameNav(session.sessionId, newBest, unlocked))
            }
        }
    }

    /** Saves the exact position (including every question's time left) and then calls [onDone]; the run can be resumed later. */
    fun leave(onDone: () -> Unit) {
        stopTicking(sync = true)
        storeCurrentRemaining()
        val saved = currentSavedGame()
        viewModelScope.launch {
            if (saved != null) ioLock.withLock { savedGames.save(saved) }
            onDone()
        }
    }

    private fun currentSavedGame(): SavedGame? {
        if (sessionId == 0L || finishing || order.isEmpty()) return null
        return SavedGame(
            sessionId = sessionId,
            level = level,
            questionIds = order.map { it.id },
            index = index,
            remainingMs = remainingByIndex.getOrElse(index) { remainingTimeMs },
            timerSeconds = timerSeconds,
            remainingByIndex = remainingByIndex.withIndex().associate { (i, v) -> i to v },
        )
    }

    private fun persist() {
        val saved = currentSavedGame() ?: return
        viewModelScope.launch { ioLock.withLock { savedGames.save(saved) } }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}
