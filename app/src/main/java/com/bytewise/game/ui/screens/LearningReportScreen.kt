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
import com.bytewise.game.data.model.name
import com.bytewise.game.ui.bytey.ByteyReactions
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.components.ConceptCard
import com.bytewise.game.ui.components.PixelCard
import com.bytewise.game.ui.components.SectionTitle
import com.bytewise.game.ui.components.StatLine
import com.bytewise.game.ui.components.adviceText
import com.bytewise.game.domain.Trend
import com.bytewise.game.viewmodel.LearningReportViewModel

/** Per-level learning report: summary, Bytey's advice, weak concepts with resources, strong concepts, study plan. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningReportScreen(
    settings: AppSettings,
    byteyTier: Int,
    onBack: () -> Unit,
    viewModel: LearningReportViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val missing by viewModel.missing.collectAsStateWithLifecycle()
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    val lang = settings.language

    LaunchedEffect(ui?.session?.sessionId, lang) {
        val r = ui ?: return@LaunchedEffect
        bytey.react(ByteyReactions.forReport(r.hasImprovement), resources)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        val r = ui
        when {
            missing -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.report_missing), textAlign = TextAlign.Center)
            }
            r == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> {
                val nameOf: (String) -> String = { id -> r.conceptsById[id]?.name(lang) ?: id }
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.BottomCenter) {
                        ByteyView(bytey, byteyTier)
                    }
                    SectionTitle(stringResource(R.string.report_summary_title))
                    PixelCard {
                        StatLine(
                            stringResource(R.string.result_correct),
                            stringResource(R.string.result_correct_value, r.session.correctAnswers, r.analysis.totalAttempts),
                        )
                        StatLine(stringResource(R.string.report_mistakes), r.analysis.totalIncorrect.toString())
                        StatLine(stringResource(R.string.report_timeouts), r.analysis.timeouts.toString())
                        if (r.analysis.correctAvgSeconds > 0f) {
                            StatLine(
                                stringResource(R.string.report_avg_correct),
                                stringResource(R.string.report_seconds, r.analysis.correctAvgSeconds),
                            )
                        }
                    }

                    SectionTitle(stringResource(R.string.report_advice_title))
                    PixelCard {
                        r.advice.forEach { a ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("•")
                                Text(adviceText(a, nameOf), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    SectionTitle(stringResource(R.string.report_weak_title))
                    val weak = r.analysis.weak
                    if (weak.isEmpty()) {
                        PixelCard { Text(stringResource(R.string.report_no_weak)) }
                    } else {
                        weak.forEach { w ->
                            val o = r.overviewById[w.conceptId]
                            ConceptCard(
                                concept = r.conceptsById[w.conceptId],
                                language = lang,
                                attempts = w.attempts,
                                correct = w.correct,
                                trend = w.trend,
                                persistent = o?.persistent == true,
                                weakLevels = o?.weakLevels.orEmpty(),
                                showDetails = true,
                            )
                        }
                    }

                    val strong = r.analysis.strong
                    if (strong.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.report_strong_title))
                        PixelCard {
                            strong.forEach { s ->
                                val improved = if (s.trend == Trend.IMPROVING) "  ↑" else ""
                                Text("✓ " + nameOf(s.conceptId) + improved, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (r.plan.items.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.report_plan_title))
                        PixelCard {
                            r.plan.items.forEachIndexed { i, item ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${i + 1}. ${nameOf(item.conceptId)}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
    }
}
