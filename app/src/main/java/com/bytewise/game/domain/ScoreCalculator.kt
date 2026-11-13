package com.bytewise.game.domain

/** Result of evaluating one answer. Pure data so the rules can be unit-tested without Android. */
data class AnswerOutcome(
    val isCorrect: Boolean,
    val basePoints: Int,
    /** True when this answer was worth double because of an earlier streak of five. */
    val multiplierApplied: Boolean,
    val earnedPoints: Int,
    val newStreak: Int,
    /** True when the NEXT question will be worth double. */
    val multiplierActiveNext: Boolean,
    /** True when this answer completed a block of five consecutive correct answers. */
    val streakMilestone: Boolean,
    /** Correct and answered within [ScoreCalculator.FAST_SECONDS]. */
    val isFast: Boolean,
)

object ScoreCalculator {
    const val STREAK_BLOCK = 5
    const val MULTIPLIER = 2
    const val FAST_SECONDS = 5f
    private const val MEDIUM_SECONDS = 10f

    /**
     * Correct within 5 s = 15, within 6-10 s = 10, otherwise 5. Anything above 10 s earns 5 because a
     * correct answer can only exist while the 20 s timer is still running (timeouts are recorded as wrong).
     */
    fun basePoints(responseSeconds: Float): Int = when {
        responseSeconds <= FAST_SECONDS -> 15
        responseSeconds <= MEDIUM_SECONDS -> 10
        else -> 5
    }

    /** After every five consecutive correct answers the next question is worth double. */
    fun isMultiplierActive(streak: Int): Boolean = streak > 0 && streak % STREAK_BLOCK == 0

    fun evaluate(streakBefore: Int, isCorrect: Boolean, responseSeconds: Float): AnswerOutcome {
        if (!isCorrect) {
            return AnswerOutcome(
                isCorrect = false, basePoints = 0, multiplierApplied = false, earnedPoints = 0,
                newStreak = 0, multiplierActiveNext = false, streakMilestone = false, isFast = false,
            )
        }
        val base = basePoints(responseSeconds)
        val applied = isMultiplierActive(streakBefore)
        val newStreak = streakBefore + 1
        return AnswerOutcome(
            isCorrect = true,
            basePoints = base,
            multiplierApplied = applied,
            earnedPoints = if (applied) base * MULTIPLIER else base,
            newStreak = newStreak,
            multiplierActiveNext = isMultiplierActive(newStreak),
            streakMilestone = newStreak % STREAK_BLOCK == 0,
            isFast = responseSeconds <= FAST_SECONDS,
        )
    }

    /** Number of consecutive correct answers at the end of [correctFlags] (oldest first). */
    fun currentStreak(correctFlags: List<Boolean>): Int {
        var streak = 0
        for (i in correctFlags.indices.reversed()) {
            if (correctFlags[i]) streak++ else break
        }
        return streak
    }
}
