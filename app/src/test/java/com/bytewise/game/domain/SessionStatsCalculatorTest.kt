package com.bytewise.game.domain

import com.bytewise.game.data.model.AnswerRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStatsCalculatorTest {
    private fun answer(correct: Boolean, points: Int) = AnswerRecord(
        sessionId = 1, questionId = "q", conceptId = "c", level = 1,
        selectedIndex = if (correct) 0 else 1, correctIndex = 0, isCorrect = correct,
        responseTimeSeconds = 3f, earnedPoints = points, wasTimeout = false, answeredAt = 0,
    )

    @Test fun countsScoreAndLongestStreak() {
        val stats = SessionStatsCalculator.calculate(
            listOf(answer(true, 15), answer(true, 10), answer(false, 0), answer(true, 5), answer(true, 5), answer(true, 15)),
        )
        assertEquals(5, stats.correct)
        assertEquals(1, stats.incorrect)
        assertEquals(50, stats.score)
        assertEquals(3, stats.longestStreak)
    }

    @Test fun emptySessionIsAllZero() {
        val stats = SessionStatsCalculator.calculate(emptyList())
        assertEquals(SessionStats(0, 0, 0, 0), stats)
    }
}
