package com.bytewise.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.R
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.ProgressState
import com.bytewise.game.data.model.name
import com.bytewise.game.domain.Trend
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.components.ConceptCard
import com.bytewise.game.ui.components.PixelCard
import com.bytewise.game.ui.components.SectionTitle
import com.bytewise.game.ui.components.StatLine
import com.bytewise.game.viewmodel.ProgressReportViewModel

/** Global progress: totals, best score per level, weakest concepts over ALL levels, study plan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    settings: AppSettings,
    byteyTier: Int,
    progress: ProgressState,
    onBack: () -> Unit,
    viewModel: ProgressReportViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    val lang = settings.language

    LaunchedEffect(ui != null, lang) {
        val r = ui ?: return@LaunchedEffect
        val improving = r.trends.values.any { it == Trend.IMPROVING }
        bytey.react(if (improving) ByteyEvent.IMPROVEMENT else ByteyEvent.LEARNING_REPORT, resources)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        val r = ui
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.BottomCenter) {
                ByteyView(bytey, byteyTier)
            }
            if (r.completedRuns == 0) {
                PixelCard { Text(stringResource(R.string.progress_empty), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                return@Column
            }
            PixelCard {
                StatLine(stringResource(R.string.progress_runs), r.completedRuns.toString())
                StatLine(stringResource(R.string.progress_answers), r.totalAnswers.toString())
                val pct = if (r.totalAnswers == 0) 0 else r.totalCorrect * 100 / r.totalAnswers
                StatLine(stringResource(R.string.progress_accuracy), stringResource(R.string.result_accuracy_value, pct))
            }

            SectionTitle(stringResource(R.string.progress_levels_title))
            PixelCard {
                for (level in 1..r.levelCount) {
                    val best = progress.bestScores[level]
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.progress_level_row, level), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            best?.toString() ?: stringResource(R.string.progress_not_played),
                            color = if (best == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            SectionTitle(stringResource(R.string.progress_weak_title))
            if (r.weaknesses.isEmpty()) {
                PixelCard { Text(stringResource(R.string.report_no_weak)) }
            } else {
                r.weaknesses.take(5).forEach { w ->
                    ConceptCard(
                        concept = r.conceptsById[w.conceptId],
                        language = lang,
                        attempts = w.attempts,
                        correct = w.correct,
                        trend = r.trends[w.conceptId] ?: Trend.NEW,
                        persistent = w.persistent,
                        weakLevels = w.weakLevels,
                        showDetails = true,
                    )
                }
            }

            if (r.plan.items.isNotEmpty()) {
                SectionTitle(stringResource(R.string.report_plan_title))
                PixelCard {
                    r.plan.items.forEachIndexed { i, item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "${i + 1}. ${r.conceptsById[item.conceptId]?.name(lang) ?: item.conceptId}",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(stringResource(R.string.report_plan_item, item.minutes))
                        }
                    }
                    Text(
                        stringResource(R.string.report_plan_total, r.plan.totalMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
