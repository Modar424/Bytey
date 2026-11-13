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
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.domain.LevelRules
import com.bytewise.game.domain.QuestionSelector
import com.bytewise.game.domain.ScoreCalculator
import com.bytewise.game.domain.SessionStatsCalculator
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
 * Runs one level: question order, 20 s timer, scoring, streak/multiplier, answer feedback, Bytey events
 * and state recovery.
 *
 * Timer rules: the time left is kept as a number that only ever shrinks by real elapsed time while the
 * timer is running. Leaving the app (ON_STOP), opening the exit dialog or rotating never resets it; while
 * paused it is frozen. An answer is accepted once only (state check on the main thread + the unique
 * (sessionId, questionId) index in Room), so a tap racing the timeout cannot be counted twice.
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
    private var sessionId = 0L
    private var level = 0
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
        startNew(questions, progress)
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

        val answers = sessions.answersForSession(session.sessionId)
        order = restored
        sessionId = session.sessionId
        level = session.level
        bestBefore = progress.bestScores[level] ?: 0
        val stats = SessionStatsCalculator.calculate(answers)
        score = stats.score
        correctCount = stats.correct
        streak = ScoreCalculator.currentStreak(answers.map { it.isCorrect })

        if (saved.index >= order.size) {
            finish()
            return true
        }
        val existing = answers.firstOrNull { it.questionId == order[saved.index].id }
        if (existing != null) {
            showAnswered(saved.index, existing)
        } else {
            showQuestion(saved.index, saved.remainingMs.coerceIn(1L, GameConfig.QUESTION_TIME_MS))
        }
        return true
    }

    private suspend fun startNew(allQuestions: List<Question>, progress: ProgressState) {
        val target = if (requestedLevel > 0) requestedLevel else nextLevelToPlay(progress)
        if (target !in progress.unlockedLevels) return fail(GameError.LOCKED)
        val levelQuestions = allQuestions.filter { it.level == target }
        if (levelQuestions.isEmpty()) return fail(GameError.NO_QUESTIONS)

        level = target
        bestBefore = progress.bestScores[target] ?: 0
        order = QuestionSelector.buildOrder(levelQuestions)
        sessionId = sessions.startSession(target)
        score = 0
        correctCount = 0
        streak = 0
        savedGames.save(SavedGame(sessionId, level, order.map { it.id }, 0, GameConfig.QUESTION_TIME_MS))
        showQuestion(0, GameConfig.QUESTION_TIME_MS)
    }

    /** Lowest unlocked level that is not completed yet; when everything is completed, the highest level. */
    private fun nextLevelToPlay(progress: ProgressState): Int {
        val unlocked = progress.unlockedLevels.sorted()
        return unlocked.firstOrNull { it !in progress.completedLevels } ?: unlocked.last()
    }

    // ---------------------------------------------------------------- showing questions

    private fun showQuestion(index: Int, remaining: Long) {
        remainingTimeMs = remaining
        _remainingMs.value = remaining
        answering = true
        _state.update {
            it.copy(
                phase = GamePhase.PLAYING,
                error = null,
                level = level,
                question = order[index],
                index = index,
                total = order.size,
                score = score,
                correctCount = correctCount,
                streak = streak,
                multiplierActive = ScoreCalculator.isMultiplierActive(streak),
                feedback = null,
            )
        }
        if (!isPaused()) startTicking()
    }

    /** Restores the feedback view of a question that was answered before the app was closed. */
    private fun showAnswered(index: Int, record: AnswerRecord) {
        answering = false
        stopTicking(sync = false)
        remainingTimeMs = 0L
        _remainingMs.value = (GameConfig.QUESTION_TIME_MS - (record.responseTimeSeconds * 1000f).toLong()).coerceAtLeast(0L)
        _state.update {
            it.copy(
                phase = GamePhase.PLAYING,
                error = null,
                level = level,
                question = order[index],
                index = index,
                total = order.size,
                score = score,
                correctCount = correctCount,
                streak = streak,
                multiplierActive = ScoreCalculator.isMultiplierActive(streak),
                feedback = AnswerFeedback(
                    selectedIndex = record.selectedIndex,
                    isCorrect = record.isCorrect,
                    timedOut = record.wasTimeout,
                    earnedPoints = record.earnedPoints,
                    multiplierApplied = false,
                ),
            )
        }
    }

    // ---------------------------------------------------------------- timer

    private fun isPaused(): Boolean = lifecyclePaused || _state.value.exitDialogVisible

    private fun startTicking() {
        stopTicking(sync = false)
        lastSyncAt = SystemClock.elapsedRealtime()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                syncRemaining()
                _remainingMs.value = remainingTimeMs
                if (remainingTimeMs <= 0L) {
                    onTimeout()
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

    /** App went to the background / came back (driven by the screen's lifecycle events). */
    fun setLifecyclePaused(paused: Boolean) {
        if (lifecyclePaused == paused) return
        lifecyclePaused = paused
        applyPause()
    }

    fun showExitDialog() {
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
                _remainingMs.value = remainingTimeMs
            }
            persist()
        } else if (answering && tickJob == null) {
            startTicking()
        }
    }

    // ---------------------------------------------------------------- answering

    fun submitAnswer(selectedIndex: Int) {
        val question = _state.value.question ?: return
        if (!answering || isPaused() || selectedIndex !in 0 until GameConfig.OPTION_COUNT) return
        answering = false
        stopTicking(sync = true)
        _remainingMs.value = remainingTimeMs
        val seconds = (GameConfig.QUESTION_TIME_MS - remainingTimeMs) / 1000f
        registerAnswer(question, selectedIndex, seconds, timedOut = false)
    }

    private fun onTimeout() {
        val question = _state.value.question ?: return
        if (!answering) return
        answering = false
        remainingTimeMs = 0L
        _remainingMs.value = 0L
        registerAnswer(question, -1, GameConfig.QUESTION_TIME_SECONDS.toFloat(), timedOut = true)
    }

    private fun registerAnswer(question: Question, selected: Int, seconds: Float, timedOut: Boolean) {
        val correct = !timedOut && selected == question.correctIndex
        val outcome = ScoreCalculator.evaluate(streak, correct, seconds)
        streak = outcome.newStreak
        score += outcome.earnedPoints
        if (correct) correctCount++
        _state.update {
            it.copy(
                score = score,
                correctCount = correctCount,
                streak = streak,
                multiplierActive = outcome.multiplierActiveNext,
                feedback = AnswerFeedback(selected, correct, timedOut, outcome.earnedPoints, outcome.multiplierApplied),
            )
        }
        _events.tryEmit(ByteyReactions.forAnswer(correct, timedOut, outcome.isFast, outcome.streakMilestone))

        val record = AnswerRecord(
            sessionId = sessionId,
            questionId = question.id,
            conceptId = question.conceptId,
            level = question.level,
            selectedIndex = if (timedOut) -1 else selected,
            correctIndex = question.correctIndex,
            isCorrect = correct,
            responseTimeSeconds = seconds,
            earnedPoints = outcome.earnedPoints,
            wasTimeout = timedOut,
            answeredAt = System.currentTimeMillis(),
        )
        viewModelScope.launch { ioLock.withLock { sessions.recordAnswer(record) } }
    }

    // ---------------------------------------------------------------- next / finish / leave

    fun next() {
        val s = _state.value
        if (s.phase != GamePhase.PLAYING || s.feedback == null) return
        val nextIndex = s.index + 1
        if (nextIndex >= order.size) {
            finish()
            return
        }
        val saved = SavedGame(sessionId, level, order.map { it.id }, nextIndex, GameConfig.QUESTION_TIME_MS)
        viewModelScope.launch { ioLock.withLock { savedGames.save(saved) } }
        showQuestion(nextIndex, GameConfig.QUESTION_TIME_MS)
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

    /** Saves the exact position (including time left) and then calls [onDone]; the run can be resumed later. */
    fun leave(onDone: () -> Unit) {
        stopTicking(sync = true)
        val saved = if (sessionId != 0L && !finishing && order.isNotEmpty()) {
            SavedGame(sessionId, level, order.map { it.id }, _state.value.index, remainingTimeMs)
        } else {
            null
        }
        viewModelScope.launch {
            if (saved != null) ioLock.withLock { savedGames.save(saved) }
            onDone()
        }
    }

    private fun persist() {
        if (sessionId == 0L || finishing || order.isEmpty()) return
        val saved = SavedGame(sessionId, level, order.map { it.id }, _state.value.index, remainingTimeMs)
        viewModelScope.launch { ioLock.withLock { savedGames.save(saved) } }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}
