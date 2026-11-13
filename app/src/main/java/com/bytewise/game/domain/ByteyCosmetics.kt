package com.bytewise.game.domain

/**
 * Free visual rewards, unlocked by reaching a level:
 * tier 0 basic, 1 programmer glasses (level 3), 2 + purple headphones (5),
 * 3 + golden coding cape (7), 4 + legendary golden crown (10).
 */
object ByteyCosmetics {
    const val MAX_TIER = 4
    private val unlockLevels = listOf(1, 3, 5, 7, 10) // index = tier

    fun tierForMaxLevel(maxUnlockedLevel: Int): Int =
        unlockLevels.indexOfLast { maxUnlockedLevel >= it }.coerceAtLeast(0)
}
