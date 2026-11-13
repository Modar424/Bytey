package com.bytewise.game.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Stored per-level learning report. The payload schema is owned by Phase 6 (LearningAnalyzer). */
@Entity(
    tableName = "learning_reports",
    foreignKeys = [
        ForeignKey(
            entity = GameSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["sessionId"], unique = true), Index("level")],
)
data class LearningReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val level: Int,
    val createdAt: Long,
    val payloadJson: String,
)

/** Point-in-time weakness state of one concept, for improvement/regression tracking (filled in Phase 6). */
@Entity(
    tableName = "weakness_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = GameSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conceptId"), Index("sessionId")],
)
data class WeaknessSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val conceptId: String,
    val createdAt: Long,
    val totalAttempts: Int,
    val totalCorrect: Int,
    /** Comma-separated level numbers in which the concept was weak, e.g. "3,5,7". */
    val weakLevelsCsv: String,
    /** IMPROVING / STABLE / REGRESSING / NEW (set by the analyzer). */
    val trend: String,
)
