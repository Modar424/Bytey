package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreCalculatorTest {
    @Test fun pointsFollowTheTimeBands() {
        assertEquals(15, ScoreCalculator.basePoints(0f))
        assertEquals(15, ScoreCalculator.basePoints(5f))
        assertEquals(10, ScoreCalculator.basePoints(5.01f))
        assertEquals(10, ScoreCalculator.basePoints(10f))
        assertEquals(5, ScoreCalculator.basePoints(10.01f))
        assertEquals(5, ScoreCalculator.basePoints(20f))
    }

    @Test fun wrongAnswerScoresNothingAndResetsStreak() {
        val o = ScoreCalculator.evaluate(streakBefore = 7, isCorrect = false, responseSeconds = 2f)
        assertEquals(0, o.earnedPoints)
        assertEquals(0, o.newStreak)
        assertFalse(o.multiplierActiveNext)
        assertFalse(o.streakMilestone)
    }

    @Test fun fifthCorrectAnswerIsNotDoubledButActivatesTheMultiplier() {
        val o = ScoreCalculator.evaluate(streakBefore = 4, isCorrect = true, responseSeconds = 3f)
        assertEquals(15, o.earnedPoints)
        assertFalse(o.multiplierApplied)
        assertEquals(5, o.newStreak)
        assertTrue(o.streakMilestone)
        assertTrue(o.multiplierActiveNext)
    }

    @Test fun questionAfterFiveInARowIsDoubledAndThenTheBonusEnds() {
        val o = ScoreCalculator.evaluate(streakBefore = 5, isCorrect = true, responseSeconds = 8f)
        assertTrue(o.multiplierApplied)
        assertEquals(20, o.earnedPoints) // 10 x 2
        assertEquals(6, o.newStreak)
        assertFalse(o.multiplierActiveNext)
    }

    @Test fun missingTheDoubledQuestionLosesTheBonus() {
        val o = ScoreCalculator.evaluate(streakBefore = 10, isCorrect = false, responseSeconds = 4f)
        assertEquals(0, o.earnedPoints)
        assertFalse(o.multiplierActiveNext)
    }

    @Test fun multiplierRepeatsEveryFive() {
        assertFalse(ScoreCalculator.isMultiplierActive(0))
        assertTrue(ScoreCalculator.isMultiplierActive(5))
        assertFalse(ScoreCalculator.isMultiplierActive(6))
        assertTrue(ScoreCalculator.isMultiplierActive(10))
        assertTrue(ScoreCalculator.isMultiplierActive(25))
    }

    @Test fun perfectFastRunScores435() {
        var streak = 0
        var total = 0
        repeat(25) {
            val o = ScoreCalculator.evaluate(streak, true, 1f)
            total += o.earnedPoints
            streak = o.newStreak
        }
        // 25 x 15 plus an extra 15 on questions 6, 11, 16 and 21.
        assertEquals(435, total)
    }

    @Test fun currentStreakCountsTrailingCorrectAnswers() {
        assertEquals(0, ScoreCalculator.currentStreak(emptyList()))
        assertEquals(0, ScoreCalculator.currentStreak(listOf(true, true, false)))
        assertEquals(3, ScoreCalculator.currentStreak(listOf(false, true, true, true)))
    }

    @Test fun fastFlagOnlyForCorrectAnswersWithinFiveSeconds() {
        assertTrue(ScoreCalculator.evaluate(0, true, 5f).isFast)
        assertFalse(ScoreCalculator.evaluate(0, true, 5.5f).isFast)
        assertFalse(ScoreCalculator.evaluate(0, false, 1f).isFast)
    }
}
