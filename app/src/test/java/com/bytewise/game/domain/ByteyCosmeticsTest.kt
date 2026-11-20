package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteyCosmeticsTest {
    @Test fun tiersUnlockAtLevels1_3_5_7_10() {
        val expected = mapOf(1 to 0, 2 to 0, 3 to 1, 4 to 1, 5 to 2, 6 to 2, 7 to 3, 9 to 3, 10 to 4, 12 to 4)
        for ((level, tier) in expected) assertEquals("level $level", tier, ByteyCosmetics.tierForMaxLevel(level))
    }
}
