package com.bytewise.game.ui.bytey

import com.bytewise.game.domain.SoundCue

/** Chooses which Bytey reaction (and sounds) fit a game moment. Pure functions, unit-tested. */
object ByteyReactions {
    /** 20 or more correct answers out of 30 counts as a high performance. */
    const val HIGH_PERFORMANCE_CORRECT = 20

    /** Priority: timeout > wrong > streak milestone > fast > plain correct. */
    fun forAnswer(isCorrect: Boolean, timedOut: Boolean, isFast: Boolean, streakMilestone: Boolean): ByteyEvent = when {
        timedOut -> ByteyEvent.TIMEOUT
        !isCorrect -> ByteyEvent.INCORRECT
        streakMilestone -> ByteyEvent.STREAK
        isFast -> ByteyEvent.FAST_CORRECT
        else -> ByteyEvent.CORRECT
    }

    /** Priority for a finished level: failed > new level unlocked > personal best > high > average. */
    fun forLevelResult(correct: Int, passed: Boolean, newBest: Boolean, unlockedNext: Boolean): ByteyEvent = when {
        !passed -> ByteyEvent.LEVEL_COMPLETE_LOW
        unlockedNext -> ByteyEvent.LEVEL_UNLOCKED
        newBest -> ByteyEvent.PERSONAL_BEST
        correct >= HIGH_PERFORMANCE_CORRECT -> ByteyEvent.LEVEL_COMPLETE_HIGH
        else -> ByteyEvent.LEVEL_COMPLETE_AVERAGE
    }

    /** Learning report: a visible improvement is celebrated, otherwise Bytey goes into learning mode. */
    fun forReport(hasImprovement: Boolean): ByteyEvent =
        if (hasImprovement) ByteyEvent.IMPROVEMENT else ByteyEvent.LEARNING_REPORT

    /** Game sound effect plus Bytey chirp for an event; events without a sound return an empty list. */
    fun cuesFor(event: ByteyEvent): List<SoundCue> = when (event) {
        ByteyEvent.CORRECT, ByteyEvent.FAST_CORRECT -> listOf(SoundCue.CORRECT, SoundCue.BYTEY_HAPPY)
        ByteyEvent.STREAK -> listOf(SoundCue.STREAK, SoundCue.BYTEY_HAPPY)
        ByteyEvent.INCORRECT -> listOf(SoundCue.WRONG, SoundCue.BYTEY_SAD)
        ByteyEvent.TIMEOUT -> listOf(SoundCue.TIMEOUT, SoundCue.BYTEY_SAD)
        ByteyEvent.LEVEL_COMPLETE_HIGH, ByteyEvent.LEVEL_COMPLETE_AVERAGE, ByteyEvent.LEVEL_UNLOCKED,
        ByteyEvent.PERSONAL_BEST, ByteyEvent.IMPROVEMENT,
        -> listOf(SoundCue.LEVEL_PASS, SoundCue.BYTEY_HAPPY)
        ByteyEvent.LEVEL_COMPLETE_LOW -> listOf(SoundCue.LEVEL_FAIL, SoundCue.BYTEY_SAD)
        // Sarcastic remarks always come with a voice; the tap voice itself is picked by the controller.
        ByteyEvent.SARCASM, ByteyEvent.ANSWER_CHANGED -> listOf(SoundCue.BYTEY_GIGGLE)
        ByteyEvent.NAV_BACK, ByteyEvent.EXIT_GAME -> listOf(SoundCue.BYTEY_HMPH)
        else -> emptyList()
    }

    /** Remarks Bytey reads aloud (his witty / sarcastic lines); plain greetings and tips stay silent. */
    fun isSpoken(event: ByteyEvent): Boolean = when (event) {
        ByteyEvent.TAP, ByteyEvent.SARCASM, ByteyEvent.NAV_BACK, ByteyEvent.ANSWER_CHANGED,
        ByteyEvent.INCORRECT, ByteyEvent.TIMEOUT, ByteyEvent.STREAK, ByteyEvent.EXIT_GAME -> true
        else -> false
    }

    /** Voices Bytey rotates through when tapped (never the same one twice in a row). */
    val tapVoices: List<SoundCue> = listOf(SoundCue.BYTEY_TAP, SoundCue.BYTEY_ROAR, SoundCue.BYTEY_GIGGLE, SoundCue.BYTEY_TAP)
}
