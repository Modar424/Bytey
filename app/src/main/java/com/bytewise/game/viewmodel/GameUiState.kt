package com.bytewise.game.viewmodel

import com.bytewise.game.data.model.Question

/** Navigation argument names and values shared by the nav graph and the view models. */
object GameArgs {
    const val LEVEL = "level"
    const val MODE = "mode"
    const val SESSION_ID = "sessionId"
    const val NEW_BEST = "best"
    const val UNLOCKED = "unlocked"

    /** "Start Game": the view model picks the level (resume the unfinished run, else the next level to play). */
    const val AUTO_LEVEL = 0
    const val NO_UNLOCK = -1

    const val MODE_AUTO = "auto"
    const val MODE_NEW = "new"
    const val MODE_RESUME = "resume"
}

enum class GamePhase { LOADING, PLAYING, FINISHING, ERROR }

enum class GameError { NO_QUESTIONS, LOCKED, SAVE_FAILED }

/** What the player did on the current question; non-null means "feedback is showing". */
data class AnswerFeedback(
    val selectedIndex: Int,
    val isCorrect: Boolean,
    val timedOut: Boolean,
    val earnedPoints: Int,
    val multiplierApplied: Boolean,
)

/**
 * Everything the gameplay screen draws except the ticking timer, which has its own flow
 * so the whole screen does not recompose ten times per second.
 */
data class GameUiState(
    val phase: GamePhase = GamePhase.LOADING,
    val error: GameError? = null,
    val level: Int = 0,
    val question: Question? = null,
    val index: Int = 0,
    val total: Int = 0,
    val score: Int = 0,
    val correctCount: Int = 0,
    val streak: Int = 0,
    /** The question on screen (or the next one, once answered) is worth double. */
    val multiplierActive: Boolean = false,
    val feedback: AnswerFeedback? = null,
    val exitDialogVisible: Boolean = false,
)

/** One-shot navigation event: the level is finished and its result can be shown. */
data class GameNav(val sessionId: Long, val newBest: Boolean, val unlockedLevel: Int)
