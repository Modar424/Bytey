package com.bytewise.game.domain

import com.bytewise.game.data.model.AnswerRecord

data class SessionStats(
    val correct: Int,
    val incorrect: Int,
    val score: Int,
    val longestStreak: Int,
)

object SessionStatsCalculator {
    /** [answers] must be in the order they were answered. */
    fun calculate(answers: List<AnswerRecord>): SessionStats {
        var streak = 0
        var longest = 0
        var correct = 0
        var score = 0
        for (a in answers) {
            score += a.earnedPoints
            if (a.isCorrect) {
                correct++
                streak++
                if (streak > longest) longest = streak
            } else {
                streak = 0
            }
        }
        return SessionStats(correct, answers.size - correct, score, longest)
    }
}
