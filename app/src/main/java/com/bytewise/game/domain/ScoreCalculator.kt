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
    /** Correct and answered within the fast band (first quarter of the selected time). */
    val isFast: Boolean,
)

/** One question's final answer, the only thing [ScoreCalculator.rescore] needs. */
data class ScoredAnswer(
    val isCorrect: Boolean,
    val responseSeconds: Float,
    /** True when the player changed this answer after submitting it (second-chance rule). */
    val changed: Boolean,
)

object ScoreCalculator {
    const val STREAK_BLOCK = 5
    const val MULTIPLIER = 2
    const val FAST_SECONDS = 5f
    private const val MEDIUM_SECONDS = 10f
    private const val DEFAULT_DURATION = 20f

    /** Points of a correct answer that was changed after it was first submitted (no speed bonus, no x2). */
    const val SECOND_CHANCE_POINTS = 5

    /** Length of the "fast" band for a level whose questions last [durationSeconds]. */
    fun fastSeconds(durationSeconds: Float = DEFAULT_DURATION): Float = FAST_SECONDS * durationSeconds / DEFAULT_DURATION

    /**
     * The speed bands scale with the selected question time so every duration is equally generous:
     * correct within the first quarter of the time = 15, within the first half = 10, otherwise 5.
     * For the default 20 s this is exactly 5 s / 10 s as before.
     */
    fun basePoints(responseSeconds: Float, durationSeconds: Float = DEFAULT_DURATION): Int {
        val scale = durationSeconds / DEFAULT_DURATION
        return when {
            responseSeconds <= FAST_SECONDS * scale -> 15
            responseSeconds <= MEDIUM_SECONDS * scale -> 10
            else -> 5
        }
    }

    /** After every five consecutive correct answers the next question is worth double. */
    fun isMultiplierActive(streak: Int): Boolean = streak > 0 && streak % STREAK_BLOCK == 0

    fun evaluate(
        streakBefore: Int,
        isCorrect: Boolean,
        responseSeconds: Float,
        durationSeconds: Float = DEFAULT_DURATION,
        changed: Boolean = false,
    ): AnswerOutcome {
        if (!isCorrect) {
            return AnswerOutcome(
                isCorrect = false, basePoints = 0, multiplierApplied = false, earnedPoints = 0,
                newStreak = 0, multiplierActiveNext = false, streakMilestone = false, isFast = false,
            )
        }
        val newStreak = streakBefore + 1
        if (changed) {
            // A correct answer reached by changing an earlier answer: flat minimum, streak still counts.
            return AnswerOutcome(
                isCorrect = true, basePoints = SECOND_CHANCE_POINTS, multiplierApplied = false,
                earnedPoints = SECOND_CHANCE_POINTS, newStreak = newStreak,
                multiplierActiveNext = isMultiplierActive(newStreak),
                streakMilestone = newStreak % STREAK_BLOCK == 0, isFast = false,
            )
        }
        val base = basePoints(responseSeconds, durationSeconds)
        val applied = isMultiplierActive(streakBefore)
        return AnswerOutcome(
            isCorrect = true,
            basePoints = base,
            multiplierApplied = applied,
            earnedPoints = if (applied) base * MULTIPLIER else base,
            newStreak = newStreak,
            multiplierActiveNext = isMultiplierActive(newStreak),
            streakMilestone = newStreak % STREAK_BLOCK == 0,
            isFast = responseSeconds <= fastSeconds(durationSeconds),
        )
    }

    /**
     * Recomputes every question's points from scratch, in question order, from the final answers only.
     * This is what makes scoring navigation-proof: each question contributes exactly once, and a changed
     * answer simply yields a different outcome list (streak and x2 bonus included).
     */
    fun rescore(answers: List<ScoredAnswer>, durationSeconds: Float = DEFAULT_DURATION): List<AnswerOutcome> {
        var streak = 0
        return answers.map { a ->
            evaluate(streak, a.isCorrect, a.responseSeconds, durationSeconds, a.changed).also { streak = it.newStreak }
        }
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
