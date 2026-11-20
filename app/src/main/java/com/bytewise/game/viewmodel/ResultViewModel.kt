package com.bytewise.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.data.model.GameSession
import com.bytewise.game.domain.LevelRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ResultUi(
    val session: GameSession,
    /** Sum of the response times (pauses and time in the background are not counted). */
    val totalSeconds: Int,
    /** The level the "Next level" button opens; null when failed or this was the last level. */
    val nextLevel: Int?,
    val newBest: Boolean,
    /** Level that this run unlocked for the first time, or null. */
    val unlockedLevel: Int?,
)

class ResultViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val container = (app as BytewiseApplication).container

    private val _ui = MutableStateFlow<ResultUi?>(null)
    val ui: StateFlow<ResultUi?> = _ui.asStateFlow()

    init {
        val sessionId = handle.get<Long>(GameArgs.SESSION_ID) ?: -1L
        val newBest = handle.get<Boolean>(GameArgs.NEW_BEST) ?: false
        val unlocked = handle.get<Int>(GameArgs.UNLOCKED) ?: GameArgs.NO_UNLOCK
        viewModelScope.launch {
            val session = container.sessionRepository.getSession(sessionId) ?: return@launch
            val answers = container.sessionRepository.answersForSession(sessionId)
            val totalLevels = container.contentRepository.levelCount()
            _ui.value = ResultUi(
                session = session,
                totalSeconds = answers.sumOf { it.responseTimeSeconds.toDouble() }.roundToInt(),
                nextLevel = if (session.passed) LevelRules.nextLevelToUnlock(session.level, totalLevels) else null,
                newBest = newBest,
                unlockedLevel = unlocked.takeIf { it != GameArgs.NO_UNLOCK },
            )
        }
    }
}
