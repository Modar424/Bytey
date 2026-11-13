package com.bytewise.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.data.model.AnswerRecord
import com.bytewise.game.data.model.Concept
import com.bytewise.game.data.model.GameSession
import com.bytewise.game.data.model.LearningReportEntity
import com.bytewise.game.data.model.WeaknessSnapshot
import com.bytewise.game.domain.Advice
import com.bytewise.game.domain.AdviceGenerator
import com.bytewise.game.domain.ConceptOverview
import com.bytewise.game.domain.LearningAnalyzer
import com.bytewise.game.domain.MultiLevelAnalyzer
import com.bytewise.game.domain.ReportCodec
import com.bytewise.game.domain.SessionAnalysis
import com.bytewise.game.domain.StudyCandidate
import com.bytewise.game.domain.StudyPlan
import com.bytewise.game.domain.StudyPlanner
import com.bytewise.game.domain.Trend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LearningReportUi(
    val session: GameSession,
    val analysis: SessionAnalysis,
    val advice: List<Advice>,
    val plan: StudyPlan,
    /** Overall (all levels) view of the concepts that were weak in this level. */
    val overviewById: Map<String, ConceptOverview>,
    val conceptsById: Map<String, Concept>,
) {
    val hasImprovement: Boolean get() = analysis.concepts.any { it.trend == Trend.IMPROVING }
}

class LearningReportViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val container = (app as BytewiseApplication).container

    private val _ui = MutableStateFlow<LearningReportUi?>(null)
    val ui: StateFlow<LearningReportUi?> = _ui.asStateFlow()

    private val _missing = MutableStateFlow(false)
    /** True when the session does not exist (nothing to show). */
    val missing: StateFlow<Boolean> = _missing.asStateFlow()

    init {
        val sessionId = handle.get<Long>(GameArgs.SESSION_ID) ?: -1L
        viewModelScope.launch {
            val session = container.sessionRepository.getSession(sessionId)
            if (session == null || session.completedAt == null) {
                _missing.value = true
                return@launch
            }
            val answers = container.sessionRepository.answersForSession(sessionId)
            val completed = container.sessionRepository.completedAnswers()
            val cutoff = answers.minOfOrNull { it.answeredAt } ?: Long.MAX_VALUE
            val history = completed.filter { it.sessionId != sessionId && it.answeredAt < cutoff }
            val overviews = MultiLevelAnalyzer.analyze(container.sessionRepository.conceptLevelStats())
            val overviewById = overviews.associateBy { it.conceptId }
            val bundle = container.contentRepository.bundle()
            val conceptsById = bundle.concepts.associateBy { it.id }

            // The stored report keeps the trends as they were when the level was finished.
            val stored = container.reportRepository.reportForSession(sessionId)
            val analysis: SessionAnalysis = stored?.let { ReportCodec.decode(it.payloadJson) }
                ?: analyzeAndStore(session, answers, history, overviewById)

            val persistentIds = overviews.filter { it.persistent }.map { it.conceptId }.toSet()
            val plan = StudyPlanner.build(
                candidates = analysis.weak.map { StudyCandidate(it.conceptId, it.priority, it.conceptId in persistentIds) },
                minutesOf = { conceptsById[it]?.estimatedMinutes ?: 0 },
                maxItems = 3,
            )
            _ui.value = LearningReportUi(
                session = session,
                analysis = analysis,
                advice = AdviceGenerator.forSession(analysis, session.passed, persistentIds),
                plan = plan,
                overviewById = overviewById,
                conceptsById = conceptsById,
            )
        }
    }

    /** Analyses the run and stores report + weakness snapshots (best-effort: a failed save still shows the report). */
    private suspend fun analyzeAndStore(
        session: GameSession,
        answers: List<AnswerRecord>,
        history: List<AnswerRecord>,
        overviewById: Map<String, ConceptOverview>,
    ): SessionAnalysis {
        val analysis = LearningAnalyzer.analyzeSession(answers, history)
        try {
            val now = System.currentTimeMillis()
            container.reportRepository.saveReport(
                LearningReportEntity(
                    sessionId = session.sessionId,
                    level = session.level,
                    createdAt = now,
                    payloadJson = ReportCodec.encode(analysis),
                ),
            )
            container.reportRepository.saveSnapshots(
                analysis.weak.map { w ->
                    val o = overviewById[w.conceptId]
                    WeaknessSnapshot(
                        sessionId = session.sessionId,
                        conceptId = w.conceptId,
                        createdAt = now,
                        totalAttempts = o?.attempts ?: w.attempts,
                        totalCorrect = o?.correct ?: w.correct,
                        weakLevelsCsv = o?.weakLevels?.joinToString(",").orEmpty(),
                        trend = w.trend.name,
                    )
                },
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
        return analysis
    }
}
