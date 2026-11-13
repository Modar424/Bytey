package com.bytewise.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.R
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.ui.bytey.ByteyReactions
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.components.PixelButton
import com.bytewise.game.ui.theme.GoldFill
import com.bytewise.game.ui.theme.LocalStatusColors
import com.bytewise.game.ui.theme.OnGoldFill
import com.bytewise.game.ui.theme.PixelFontFamily
import com.bytewise.game.utils.SoundEffects
import com.bytewise.game.viewmodel.ResultViewModel


/**
 * Basic results page of a finished level (score, accuracy, streak, time, pass/unlock status).
 * The "Learning report" button opens the detailed analysis (Phase 6); visual polish is Phase 7.
 */
@Composable
fun ResultScreen(
    settings: AppSettings,
    byteyTier: Int,
    onNextLevel: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onReport: (Long) -> Unit,
    onMenu: () -> Unit,
    viewModel: ResultViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val bytey = rememberByteyController()
    val sfx = remember { SoundEffects() }
    val resources by rememberUpdatedState(LocalContext.current.resources)
    val latestSettings by rememberUpdatedState(settings)
    val cs = MaterialTheme.colorScheme

    DisposableEffect(Unit) { onDispose { sfx.release() } }
    BackHandler { onMenu() }

    val result = ui
    LaunchedEffect(result?.session?.sessionId) {
        val r = result ?: return@LaunchedEffect
        val event = ByteyReactions.forLevelResult(
            correct = r.session.correctAnswers,
            passed = r.session.passed,
            newBest = r.newBest,
            unlockedNext = r.unlockedLevel != null,
        )
        ByteyReactions.cuesFor(event).forEach { sfx.play(it, latestSettings) }
        bytey.react(event, resources)
    }

    if (result == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val session = result.session
    val status = LocalStatusColors.current
    val statusColor = if (session.passed) status.good else status.bad
    val accuracy = if (session.correctAnswers + session.incorrectAnswers == 0) 0
    else (session.correctAnswers * 100f / (session.correctAnswers + session.incorrectAnswers)).toInt()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.result_title, session.level),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Box(Modifier.fillMaxWidth().heightIn(min = 280.dp), contentAlignment = Alignment.BottomCenter) {
            ByteyView(bytey, byteyTier)
        }
        Text(
            text = if (session.passed) stringResource(R.string.result_passed)
            else stringResource(R.string.result_failed, GameConfig.PASS_THRESHOLD),
            modifier = Modifier
                .fillMaxWidth()
                .background(statusColor.copy(alpha = 0.14f), RectangleShape)
                .border(3.dp, statusColor, RectangleShape)
                .padding(12.dp),
            color = cs.onSurface,
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        if (result.newBest) Banner(stringResource(R.string.result_new_best))
        result.unlockedLevel?.let { Banner(stringResource(R.string.result_unlocked, it)) }

        Column(
            Modifier
                .fillMaxWidth()
                .background(cs.surface, RectangleShape)
                .border(3.dp, cs.outline, RectangleShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatRow(stringResource(R.string.result_score), session.score.toString())
            StatRow(
                stringResource(R.string.result_correct),
                stringResource(R.string.result_correct_value, session.correctAnswers, session.correctAnswers + session.incorrectAnswers),
            )
            StatRow(stringResource(R.string.result_accuracy), stringResource(R.string.result_accuracy_value, accuracy))
            StatRow(stringResource(R.string.result_streak), session.longestStreak.toString())
            StatRow(
                stringResource(R.string.result_time),
                stringResource(R.string.result_time_value, result.totalSeconds / 60, result.totalSeconds % 60),
            )
        }

        result.nextLevel?.let { next ->
            PixelButton(
                text = stringResource(R.string.result_next_level),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                container = cs.primary,
                onContainer = cs.onPrimary,
                onClick = { onNextLevel(next) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PixelButton(
            text = stringResource(R.string.report_button),
            icon = Icons.Filled.Info,
            container = GoldFill,
            onContainer = OnGoldFill,
            onClick = { onReport(session.sessionId) },
            modifier = Modifier.fillMaxWidth(),
        )
        PixelButton(
            text = stringResource(R.string.result_retry),
            icon = Icons.Filled.Refresh,
            container = cs.secondary,
            onContainer = cs.onSecondary,
            onClick = { onRetry(session.level) },
            modifier = Modifier.fillMaxWidth(),
        )
        PixelButton(
            text = stringResource(R.string.result_menu),
            icon = Icons.Filled.Home,
            container = cs.surfaceVariant,
            onContainer = cs.onSurface,
            onClick = onMenu,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Banner(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().background(GoldFill, RectangleShape).padding(10.dp),
        color = OnGoldFill,
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = PixelFontFamily, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}
