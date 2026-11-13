package com.bytewise.game.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions", indices = [Index("level")])
data class GameSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val level: Int,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val longestStreak: Int = 0,
    val startedAt: Long,
    /** null while the session is still in progress. */
    val completedAt: Long? = null,
    val passed: Boolean = false,
)
