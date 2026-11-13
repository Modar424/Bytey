package com.bytewise.game.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One answered (or timed-out) question. The unique (sessionId, questionId) index makes
 * duplicate submissions impossible at the database level.
 */
@Entity(
    tableName = "answer_records",
    foreignKeys = [
        ForeignKey(
            entity = GameSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId", "questionId"], unique = true),
        Index("conceptId"),
    ],
)
data class AnswerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionId: String,
    val conceptId: String,
    val level: Int,
    /** -1 when the question timed out. */
    val selectedIndex: Int,
    val correctIndex: Int,
    val isCorrect: Boolean,
    val responseTimeSeconds: Float,
    val earnedPoints: Int,
    val wasTimeout: Boolean,
    val answeredAt: Long,
)

/** Aggregated attempts for one concept in one level, over completed sessions only. */
data class ConceptLevelStat(
    val conceptId: String,
    val level: Int,
    val attempts: Int,
    val correct: Int,
)

/** Aggregated attempts for one concept inside a single session. */
data class ConceptSessionStat(
    val conceptId: String,
    val attempts: Int,
    val correct: Int,
)
