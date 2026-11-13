package com.bytewise.game.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.ConceptLevelStat
import com.bytewise.game.data.model.ConceptSessionStat

@Dao
interface AnswerRecordDao {
    /** Returns -1 when the (sessionId, questionId) pair already exists (duplicate ignored). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: AnswerRecord): Long

    @Query("SELECT * FROM answer_records WHERE sessionId = :sessionId ORDER BY id")
    suspend fun getBySession(sessionId: Long): List<AnswerRecord>

    @Query(
        "SELECT conceptId AS conceptId, COUNT(*) AS attempts, SUM(isCorrect) AS correct " +
            "FROM answer_records WHERE sessionId = :sessionId GROUP BY conceptId",
    )
    suspend fun conceptStatsForSession(sessionId: Long): List<ConceptSessionStat>

    /** Concept statistics across all COMPLETED sessions, per level (weighted: raw counts, not percentages). */
    @Query(
        "SELECT a.conceptId AS conceptId, a.level AS level, COUNT(*) AS attempts, SUM(a.isCorrect) AS correct " +
            "FROM answer_records a INNER JOIN game_sessions s ON s.sessionId = a.sessionId " +
            "WHERE s.completedAt IS NOT NULL GROUP BY a.conceptId, a.level",
    )
    suspend fun conceptLevelStats(): List<ConceptLevelStat>

    /** Every answer of every COMPLETED session, oldest first (history and trend analysis). */
    @Query(
        "SELECT a.* FROM answer_records a INNER JOIN game_sessions s ON s.sessionId = a.sessionId " +
            "WHERE s.completedAt IS NOT NULL ORDER BY a.answeredAt, a.id",
    )
    suspend fun completedAnswers(): List<AnswerRecord>
}
