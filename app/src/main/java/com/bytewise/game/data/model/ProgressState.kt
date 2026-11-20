package com.bytewise.game.data.model

data class ProgressState(
    val unlockedLevels: Set<Int> = setOf(1),
    val completedLevels: Set<Int> = emptySet(),
    val bestScores: Map<Int, Int> = emptyMap(),
)
