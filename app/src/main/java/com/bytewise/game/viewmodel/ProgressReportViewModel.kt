package com.bytewise.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.data.model.Concept
import com.bytewise.game.domain.ConceptOverview
import com.bytewise.game.domain.MultiLevelAnalyzer
import com.bytewise.game.domain.StudyCandidate
import com.bytewise.game.domain.StudyPlan
import com.bytewise.game.domain.StudyPlanner
import com.bytewise.game.domain.Trend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressReportUi(
    /** Concepts with mistakes over all completed sessions, most urgent first. */
    val weaknesses: List<ConceptOverview>,
    val trends: Map<String, Trend>,
    val plan: StudyPlan,
    val conceptsById: Map<String, Concept>,
    val completedRuns: Int,
    val totalAnswers: Int,
    val totalCorrect: Int,
    val levelCount: Int,
)

class ProgressReportViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as BytewiseApplication).container

    private val _ui = MutableStateFlow<ProgressReportUi?>(null)
    val ui: StateFlow<ProgressReportUi?> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val answers = container.sessionRepository.completedAnswers()
            val stats = container.sessionRepository.conceptLevelStats()
            val overviews = MultiLevelAnalyzer.analyze(stats)
            val concepts = container.contentRepository.bundle().concepts.associateBy { it.id }
            _ui.value = ProgressReportUi(
                weaknesses = overviews,
                trends = MultiLevelAnalyzer.trends(answers),
                plan = StudyPlanner.build(
                    overviews.map { StudyCandidate(it.conceptId, it.priority, it.persistent) },
                    minutesOf = { concepts[it]?.estimatedMinutes ?: 0 },
                ),
                conceptsById = concepts,
                completedRuns = answers.map { it.sessionId }.distinct().size,
                totalAnswers = answers.size,
                totalCorrect = answers.count { it.isCorrect },
                levelCount = container.contentRepository.levelCount(),
            )
        }
    }
}
