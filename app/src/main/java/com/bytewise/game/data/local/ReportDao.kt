package com.bytewise.game.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bytewise.game.data.model.LearningReportEntity
import com.bytewise.game.data.model.WeaknessSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(report: LearningReportEntity): Long

    @Query("SELECT * FROM learning_reports WHERE sessionId = :sessionId")
    suspend fun getReportForSession(sessionId: Long): LearningReportEntity?

    @Query("SELECT * FROM learning_reports ORDER BY createdAt DESC")
    fun observeReports(): Flow<List<LearningReportEntity>>

    @Insert
    suspend fun insertSnapshots(snapshots: List<WeaknessSnapshot>)

    @Query("SELECT * FROM weakness_snapshots WHERE conceptId = :conceptId ORDER BY createdAt")
    suspend fun snapshotsForConcept(conceptId: String): List<WeaknessSnapshot>
}
