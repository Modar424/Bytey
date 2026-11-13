package com.bytewise.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.ProgressState
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.components.isLandscape
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.theme.LocalStatusColors
import com.bytewise.game.ui.theme.PixelFontFamily
import com.bytewise.game.viewmodel.LevelsViewModel

private val CompletedColor: Color @Composable get() = LocalStatusColors.current.good

/** Level selection: 2-column grid with locked / open / completed status and best score. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelsScreen(
    progress: ProgressState,
    byteyTier: Int,
    language: AppLanguage,
    onBack: () -> Unit,
    onPlay: (level: Int, resume: Boolean) -> Unit,
    viewModel: LevelsViewModel = viewModel(),
) {
    val levelCount by viewModel.levelCount.collectAsStateWithLifecycle()
    val unfinishedLevel by viewModel.unfinishedLevel.collectAsStateWithLifecycle()
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    var pendingLevel by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshUnfinished() }
    LaunchedEffect(language) { bytey.react(ByteyEvent.LEVELS_TIP, resources) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.levels_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    stringResource(R.string.levels_hint_locked, GameConfig.PASS_THRESHOLD, GameConfig.QUESTIONS_PER_LEVEL),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isLandscape()) 4 else 2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items((1..levelCount).toList()) { level ->
                        val unlocked = level in progress.unlockedLevels
                        LevelCard(
                            level = level,
                            unlocked = unlocked,
                            completed = level in progress.completedLevels,
                            bestScore = progress.bestScores[level],
                            onClick = {
                                if (unlocked) {
                                    if (unfinishedLevel == level) pendingLevel = level else onPlay(level, false)
                                }
                            },
                        )
                    }
                }
            }
            ByteyView(bytey, byteyTier, Modifier.align(Alignment.BottomEnd).padding(12.dp))
        }
    }

    pendingLevel?.let { level ->
        AlertDialog(
            onDismissRequest = { pendingLevel = null },
            title = { Text(stringResource(R.string.level_resume_title)) },
            text = { Text(stringResource(R.string.level_resume_message, level)) },
            confirmButton = {
                TextButton(onClick = { pendingLevel = null; onPlay(level, true) }) {
                    Text(stringResource(R.string.level_resume))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLevel = null; onPlay(level, false) }) {
                    Text(stringResource(R.string.level_restart))
                }
            },
        )
    }
}

@Composable
private fun LevelCard(level: Int, unlocked: Boolean, completed: Boolean, bestScore: Int?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val accent = when {
        completed -> CompletedColor
        unlocked -> cs.primary
        else -> cs.outline
    }
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .background(cs.surface, RectangleShape)
            .border(3.dp, accent, RectangleShape)
            .clickable(enabled = unlocked, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.level_number, level),
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = if (unlocked) cs.onSurface else cs.onSurface.copy(alpha = 0.65f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    completed -> Icons.Filled.CheckCircle
                    unlocked -> Icons.Filled.PlayArrow
                    else -> Icons.Filled.Lock
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(
                    when {
                        completed -> R.string.level_status_completed
                        unlocked -> R.string.level_status_open
                        else -> R.string.level_status_locked
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            if (bestScore != null) stringResource(R.string.level_best_score, bestScore) else stringResource(R.string.level_no_score),
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}
