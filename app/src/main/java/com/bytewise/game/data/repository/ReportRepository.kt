package com.bytewise.game.data.repository

import com.bytewise.game.data.local.BytewiseDatabase
import com.bytewise.game.data.model.LearningReportEntity
import com.bytewise.game.data.model.WeaknessSnapshot
import kotlinx.coroutines.flow.Flow

/** Persistence for reports and weakness snapshots. */
class ReportRepository(db: BytewiseDatabase) {
    private val dao = db.reportDao()

    suspend fun saveReport(report: LearningReportEntity) = dao.upsertReport(report)
    suspend fun reportForSession(sessionId: Long): LearningReportEntity? = dao.getReportForSession(sessionId)
    fun observeReports(): Flow<List<LearningReportEntity>> = dao.observeReports()

    suspend fun saveSnapshots(snapshots: List<WeaknessSnapshot>) = dao.insertSnapshots(snapshots)
    suspend fun snapshotsForConcept(conceptId: String): List<WeaknessSnapshot> = dao.snapshotsForConcept(conceptId)
}
