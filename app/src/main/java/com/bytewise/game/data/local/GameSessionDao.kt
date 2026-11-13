package com.bytewise.game.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bytewise.game.data.model.GameSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Insert
    suspend fun insert(session: GameSession): Long

    @Update
    suspend fun update(session: GameSession)

    @Query("SELECT * FROM game_sessions WHERE sessionId = :id")
    suspend fun getById(id: Long): GameSession?

    @Query("SELECT * FROM game_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<GameSession>>

    @Query("SELECT * FROM game_sessions WHERE level = :level AND completedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeCompletedByLevel(level: Int): Flow<List<GameSession>>

    @Query("SELECT * FROM game_sessions WHERE completedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeCompleted(): Flow<List<GameSession>>

    /** Most recent in-progress session (used for state recovery in Phase 5). */
    @Query("SELECT * FROM game_sessions WHERE completedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getUnfinished(): GameSession?
}
