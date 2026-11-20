package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAndNavigationTest {
    @Test fun timerOptionsAre20To90InTens() {
        assertEquals(listOf(20, 30, 40, 50, 60, 70, 80, 90), GameConfig.TIMER_OPTIONS_SECONDS)
        assertEquals(20, GameConfig.QUESTION_TIME_SECONDS)
        assertEquals(20, GameConfig.normalizeTimer(25))
        assertEquals(60, GameConfig.normalizeTimer(60))
    }

    @Test fun speedBandsScaleWithTheSelectedDuration() {
        assertEquals(15, ScoreCalculator.basePoints(5f, 20f))
        assertEquals(10, ScoreCalculator.basePoints(5.1f, 20f))
        assertEquals(15, ScoreCalculator.basePoints(22.5f, 90f))
        assertEquals(10, ScoreCalculator.basePoints(45f, 90f))
        assertEquals(5, ScoreCalculator.basePoints(45.5f, 90f))
    }

    @Test fun rescoreCountsEachQuestionOnceAndMatchesIncrementalScoring() {
        val answers = List(6) { ScoredAnswer(true, 3f, false) }
        val outcomes = ScoreCalculator.rescore(answers)
        assertEquals(listOf(15, 15, 15, 15, 15, 30), outcomes.map { it.earnedPoints })
    }

    @Test fun changingAnAnswerRecalculatesInsteadOfAdding() {
        val before = ScoreCalculator.rescore(listOf(ScoredAnswer(false, 4f, false), ScoredAnswer(true, 4f, false)))
        assertEquals(15, before.sumOf { it.earnedPoints })
        val after = ScoreCalculator.rescore(listOf(ScoredAnswer(true, 4f, true), ScoredAnswer(true, 4f, false)))
        assertEquals(5 + 15, after.sumOf { it.earnedPoints }) // second-chance answer: flat 5, no duplicate entry
        assertEquals(2, after.size)
    }

    @Test fun changingCorrectToWrongRemovesItsPointsAndBreaksTheStreak() {
        val o = ScoreCalculator.rescore(List(5) { ScoredAnswer(true, 2f, false) } + ScoredAnswer(true, 2f, false))
        assertEquals(75 + 30, o.sumOf { it.earnedPoints })
        val changed = ScoreCalculator.rescore(
            listOf(ScoredAnswer(false, 2f, true)) + List(5) { ScoredAnswer(true, 2f, false) },
        )
        assertEquals(75, changed.sumOf { it.earnedPoints })
    }

    @Test fun navigationRules() {
        assertFalse(NavigationRules.canGoPrevious(0))
        assertTrue(NavigationRules.canGoPrevious(1))
        assertFalse(NavigationRules.canGoNext(3, 25, currentResolved = false))
        assertTrue(NavigationRules.canGoNext(3, 25, currentResolved = true))
        assertFalse(NavigationRules.canGoNext(24, 25, currentResolved = true))
        assertTrue(NavigationRules.canFinish(24, 25, 25))
        assertFalse(NavigationRules.canFinish(24, 25, 24))
    }

    @Test fun expiredQuestionsStayLocked() {
        assertFalse(NavigationRules.canChangeAnswer(answered = true, timedOut = true, remainingMs = 0))
        assertFalse(NavigationRules.canChangeAnswer(answered = false, timedOut = false, remainingMs = 5000))
        assertTrue(NavigationRules.canChangeAnswer(answered = true, timedOut = false, remainingMs = 5000))
    }

    @Test fun sarcasmGateLimitsFrequency() {
        val gate = SarcasmGate(minGapMs = 1000)
        assertTrue(gate.tryAcquire(0))
        assertFalse(gate.tryAcquire(500))
        assertTrue(gate.tryAcquire(1500))
    }
}
