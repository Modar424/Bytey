package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRulesTest {
    @Test fun passMarkIsFifteenOfTwentyFive() {
        assertFalse(LevelRules.isPassed(14))
        assertTrue(LevelRules.isPassed(15))
    }

    @Test fun lastLevelUnlocksNothing() {
        assertEquals(4, LevelRules.nextLevelToUnlock(3, 10))
        assertNull(LevelRules.nextLevelToUnlock(10, 10))
    }

    @Test fun difficultyTableMatchesSpec() {
        assertEquals(1..2, GameConfig.difficultyRange(1))
        assertEquals(7..8, GameConfig.difficultyRange(10))
        assertEquals(7..8, GameConfig.difficultyRange(11))
    }
}
