package com.bytewise.game.data.repository

import androidx.room.withTransaction
import com.bytewise.game.data.local.BytewiseDatabase
import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.ConceptLevelStat
import com.bytewise.game.data.model.GameSession
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.domain.LevelRules
import com.bytewise.game.domain.SessionStatsCalculator
import kotlinx.coroutines.flow.Flow

class SessionRepository(
    private val db: BytewiseDatabase,
    private val progress: ProgressRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val sessions get() = db.gameSessionDao()
    private val answers get() = db.answerRecordDao()

    suspend fun startSession(level: Int): Long =
        sessions.insert(GameSession(level = level, startedAt = clock()))

    /** Returns false if this question was already answered in the session (duplicate ignored). */
    suspend fun recordAnswer(record: AnswerRecord): Boolean = answers.insert(record) != -1L

    /**
     * Finalises a session: totals are derived from the stored answers (single source of truth),
     * then best score / unlocking are applied exactly once. Calling it again on a completed
     * session returns the stored session without re-applying progress.
     */
    suspend fun completeSession(
        sessionId: Long,
        totalLevels: Int = GameConfig.INITIAL_LEVEL_COUNT,
    ): GameSession? {
        var newlyCompleted = false
        val result = db.withTransaction {
            val session = sessions.getById(sessionId) ?: return@withTransaction null
            if (session.completedAt != null) return@withTransaction session
            val stats = SessionStatsCalculator.calculate(answers.getBySession(sessionId))
            val updated = session.copy(
                score = stats.score,
                correctAnswers = stats.correct,
                incorrectAnswers = stats.incorrect,
                longestStreak = stats.longestStreak,
                completedAt = clock(),
                passed = LevelRules.isPassed(stats.correct),
            )
            sessions.update(updated)
            newlyCompleted = true
            updated
        }
        if (result != null && newlyCompleted) {
            progress.recordLevelResult(result.level, result.score, result.passed, totalLevels)
        }
        return result
    }

    suspend fun getSession(sessionId: Long): GameSession? = sessions.getById(sessionId)
    suspend fun getUnfinishedSession(): GameSession? = sessions.getUnfinished()
    suspend fun answersForSession(sessionId: Long): List<AnswerRecord> = answers.getBySession(sessionId)
    suspend fun conceptLevelStats(): List<ConceptLevelStat> = answers.conceptLevelStats()
    suspend fun completedAnswers(): List<AnswerRecord> = answers.completedAnswers()

    fun observeAllSessions(): Flow<List<GameSession>> = sessions.observeAll()
    fun observeCompletedSessions(): Flow<List<GameSession>> = sessions.observeCompleted()
    fun observeCompletedByLevel(level: Int): Flow<List<GameSession>> = sessions.observeCompletedByLevel(level)
}
