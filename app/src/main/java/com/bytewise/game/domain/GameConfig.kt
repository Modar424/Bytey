package com.bytewise.game.domain

object GameConfig {
    const val QUESTIONS_PER_LEVEL = 25
    const val QUESTIONS_PER_CATEGORY = 5
    const val PASS_THRESHOLD = 15
    const val QUESTION_TIME_SECONDS = 20
    const val QUESTION_TIME_MS = QUESTION_TIME_SECONDS * 1000L
    const val OPTION_COUNT = 4
    const val INITIAL_LEVEL_COUNT = 10
    const val MAX_CONCEPT_RESOURCES = 5

    private val difficultyByLevel = listOf(1..2, 2..3, 3..3, 3..4, 4..4, 4..5, 5..5, 5..6, 6..7, 7..8)

    /** Allowed difficulty (inclusive) for a level; levels beyond the table reuse the last range. */
    fun difficultyRange(level: Int): IntRange =
        difficultyByLevel[(level - 1).coerceIn(0, difficultyByLevel.lastIndex)]
}

object LevelRules {
    fun isPassed(correctAnswers: Int): Boolean = correctAnswers >= GameConfig.PASS_THRESHOLD

    /** The level unlocked by passing [level], or null when it is the last one. */
    fun nextLevelToUnlock(level: Int, totalLevels: Int): Int? =
        if (level + 1 <= totalLevels) level + 1 else null
}
