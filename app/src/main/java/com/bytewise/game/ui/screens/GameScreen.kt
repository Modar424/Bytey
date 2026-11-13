package com.bytewise.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.bytewise.game.domain.SoundCue
import com.bytewise.game.ui.bytey.ByteySprites
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.QuestionCategory
import com.bytewise.game.data.model.explanation
import com.bytewise.game.data.model.options
import com.bytewise.game.data.model.text
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.domain.TickPolicy
import com.bytewise.game.domain.TimerZone
import com.bytewise.game.ui.bytey.ByteyController
import com.bytewise.game.ui.bytey.ByteyReactions
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.components.PixelButton
import com.bytewise.game.ui.components.isLandscape
import com.bytewise.game.ui.theme.GoldFill
import com.bytewise.game.ui.theme.LocalStatusColors
import com.bytewise.game.ui.theme.OnGoldFill
import com.bytewise.game.ui.theme.PixelFontFamily
import com.bytewise.game.utils.SoundEffects
import com.bytewise.game.viewmodel.AnswerFeedback
import com.bytewise.game.viewmodel.GameError
import com.bytewise.game.viewmodel.GameNav
import com.bytewise.game.viewmodel.GamePhase
import com.bytewise.game.viewmodel.GameUiState
import com.bytewise.game.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.ceil

private val CorrectColor: Color @Composable get() = LocalStatusColors.current.good
private val WrongColor: Color @Composable get() = LocalStatusColors.current.bad
private val WarnColor: Color @Composable get() = LocalStatusColors.current.warn
private val FireColor: Color @Composable get() = LocalStatusColors.current.fire

@Composable
fun GameScreen(
    settings: AppSettings,
    byteyTier: Int,
    onExit: () -> Unit,
    onFinished: (GameNav) -> Unit,
    viewModel: GameViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bytey = rememberByteyController()
    val sfx = remember { SoundEffects() }
    val resources by rememberUpdatedState(LocalContext.current.resources)
    val latestSettings by rememberUpdatedState(settings)
    val latestOnFinished by rememberUpdatedState(onFinished)

    DisposableEffect(Unit) { onDispose { sfx.release() } }

    // Leaving the app freezes the timer; coming back continues with the time that was left.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.setLifecyclePaused(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setLifecyclePaused(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            ByteyReactions.cuesFor(event).forEach { sfx.play(it, latestSettings) }
            bytey.react(event, resources)
        }
    }
    LaunchedEffect(viewModel) { viewModel.navigation.collect { latestOnFinished(it) } }

    // A soft tick for each of the last five seconds (game-sound toggle applies through SoundPolicy).
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(viewModel) {
        var lastSecond = Int.MAX_VALUE
        viewModel.remainingMs.collect { ms ->
            val sec = ceil(ms / 1000.0).toInt()
            if (sec > lastSecond) lastSecond = Int.MAX_VALUE // a new question started
            if (sec != lastSecond) {
                lastSecond = sec
                val s = latestState
                if (TickPolicy.shouldTick(sec) && s.phase == GamePhase.PLAYING && s.feedback == null && !s.exitDialogVisible) {
                    sfx.play(SoundCue.TICK, latestSettings)
                }
            }
        }
    }

    BackHandler(enabled = state.phase == GamePhase.PLAYING) {
        if (state.exitDialogVisible) viewModel.hideExitDialog() else viewModel.showExitDialog()
    }

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        when (state.phase) {
            GamePhase.LOADING, GamePhase.FINISHING -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.game_loading))
            }
            GamePhase.ERROR -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(
                        when (state.error) {
                            GameError.LOCKED -> R.string.game_error_locked
                            GameError.SAVE_FAILED -> R.string.game_error_save
                            else -> R.string.game_error_empty
                        },
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onExit) { Text(stringResource(R.string.action_back)) }
            }
            GamePhase.PLAYING -> PlayingContent(
                state = state,
                language = settings.language,
                remaining = viewModel.remainingMs,
                bytey = bytey,
                byteyTier = byteyTier,
                onClose = viewModel::showExitDialog,
                onAnswer = viewModel::submitAnswer,
                onNext = viewModel::next,
            )
        }
    }

    if (state.exitDialogVisible && state.phase == GamePhase.PLAYING) {
        AlertDialog(
            onDismissRequest = viewModel::hideExitDialog,
            title = { Text(stringResource(R.string.game_exit_title)) },
            text = { Text(stringResource(R.string.game_exit_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.leave(onExit) }) { Text(stringResource(R.string.game_exit_leave)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideExitDialog) { Text(stringResource(R.string.game_exit_keep)) }
            },
        )
    }
}

@Composable
private fun PlayingContent(
    state: GameUiState,
    language: AppLanguage,
    remaining: StateFlow<Long>,
    bytey: ByteyController,
    byteyTier: Int,
    onClose: () -> Unit,
    onAnswer: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val question = state.question ?: return
    val feedback = state.feedback
    val cs = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val labels = stringArrayResource(R.array.option_labels)
    val answered = state.index + if (feedback != null) 1 else 0

    LaunchedEffect(state.index) { scroll.scrollTo(0) }
    LaunchedEffect(state.index, feedback != null) {
        if (feedback != null) {
            delay(250)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.game_exit_title))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.game_level_title, state.level),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.game_question_counter, state.index + 1, state.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            StatChip(Icons.Filled.Star, state.score.toString(), WarnColor)
            Spacer(Modifier.width(8.dp))
            StatChip(Icons.Filled.LocalFireDepartment, state.streak.toString(), FireColor)
            Spacer(Modifier.width(8.dp))
        }

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(
                progress = { answered.toFloat() / state.total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = cs.primary,
                trackColor = cs.surfaceVariant,
            )
            TimerBar(remaining)
            if (state.multiplierActive) {
                Text(
                    stringResource(if (feedback == null) R.string.game_multiplier_active else R.string.game_multiplier_next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GoldFill, RectangleShape)
                        .padding(vertical = 4.dp),
                    color = OnGoldFill,
                    fontFamily = PixelFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        val questionBlock: @Composable ColumnScope.() -> Unit = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(cs.surface, RectangleShape)
                    .border(3.dp, cs.outline, RectangleShape)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    categoryLabel(question.category),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    question.text(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        val optionsBlock: @Composable ColumnScope.() -> Unit = {
            question.options(language).forEachIndexed { i, option ->
                val visual = when {
                    feedback == null -> OptionVisual.NEUTRAL
                    i == question.correctIndex -> OptionVisual.CORRECT
                    i == feedback.selectedIndex -> OptionVisual.WRONG
                    else -> OptionVisual.DIMMED
                }
                OptionCard(
                    label = labels.getOrElse(i) { (i + 1).toString() },
                    text = option,
                    visual = visual,
                    enabled = feedback == null,
                    onClick = { onAnswer(i) },
                )
            }
        }
        val feedbackBlock: @Composable ColumnScope.() -> Unit = {
            if (feedback != null) {
                FeedbackBanner(feedback)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(cs.surfaceVariant, RectangleShape)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        stringResource(R.string.game_explanation),
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(question.explanation(language), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (isLandscape()) {
                // Wide and short: the question and its explanation on the left, the four options on the right.
                val rightScroll = rememberScrollState()
                LaunchedEffect(state.index) { rightScroll.scrollTo(0) }
                Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        questionBlock()
                        feedbackBlock()
                        Spacer(Modifier.height(130.dp))
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rightScroll),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) { optionsBlock() }
                }
                ByteyView(bytey, byteyTier, Modifier.align(Alignment.BottomStart).padding(8.dp))
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    questionBlock()
                    optionsBlock()
                    feedbackBlock()
                    // Room so the last card can scroll above Bytey in the corner.
                    Spacer(Modifier.height(130.dp))
                }
                ByteyView(bytey, byteyTier, Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }

        if (feedback != null) {
            val last = state.index + 1 >= state.total
            PixelButton(
                text = stringResource(if (last) R.string.game_finish else R.string.game_next),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                container = cs.primary,
                onContainer = cs.onPrimary,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun TimerBar(remainingFlow: StateFlow<Long>) {
    val remaining by remainingFlow.collectAsStateWithLifecycle()
    val fraction = (remaining.toFloat() / GameConfig.QUESTION_TIME_MS).coerceIn(0f, 1f)
    val color = when (TimerZone.of(fraction)) {
        TimerZone.GREEN -> CorrectColor
        TimerZone.YELLOW -> WarnColor
        TimerZone.RED -> WrongColor
    }
    val seconds = ceil(remaining / 1000.0).toInt()
    val reduceMotion = ByteySprites.isReducedMotion(LocalContext.current)
    val pulse = rememberInfiniteTransition(label = "timerPulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "timerAlpha",
    )
    val urgent = TimerZone.of(fraction) == TimerZone.RED && remaining > 0 && !reduceMotion
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.weight(1f).height(14.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            stringResource(R.string.game_seconds_left, seconds),
            modifier = Modifier.width(56.dp).graphicsLayer { alpha = if (urgent) pulse.value else 1f },
            color = color,
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .background(cs.surface, RectangleShape)
            .border(2.dp, cs.outline, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, fontFamily = PixelFontFamily, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

private enum class OptionVisual { NEUTRAL, CORRECT, WRONG, DIMMED }

@Composable
private fun OptionCard(label: String, text: String, visual: OptionVisual, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val reduceMotion = ByteySprites.isReducedMotion(LocalContext.current)
    val targetAccent = when (visual) {
        OptionVisual.CORRECT -> CorrectColor
        OptionVisual.WRONG -> WrongColor
        else -> cs.outline
    }
    val targetFill = when (visual) {
        OptionVisual.CORRECT -> CorrectColor.copy(alpha = 0.22f)
        OptionVisual.WRONG -> WrongColor.copy(alpha = 0.22f)
        else -> cs.surface
    }
    val duration = if (reduceMotion) 0 else 220
    val accent by animateColorAsState(targetAccent, tween(duration), label = "accent")
    val fill by animateColorAsState(targetFill, tween(duration), label = "fill")

    // Correct: a quick pulse. Wrong: a short horizontal shake. Both are skipped when system animations are off.
    val scale = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(visual) {
        if (reduceMotion) return@LaunchedEffect
        when (visual) {
            OptionVisual.CORRECT -> {
                scale.animateTo(1.04f, tween(120))
                scale.animateTo(1f, tween(160))
            }
            OptionVisual.WRONG -> {
                repeat(3) {
                    shake.animateTo(10f, tween(50))
                    shake.animateTo(-10f, tween(50))
                }
                shake.animateTo(0f, tween(50))
            }
            else -> Unit
        }
    }

    val stateText = when (visual) {
        OptionVisual.CORRECT -> stringResource(R.string.a11y_option_correct)
        OptionVisual.WRONG -> stringResource(R.string.a11y_option_wrong)
        else -> null
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shake.value
            }
            .semantics(mergeDescendants = true) { if (stateText != null) stateDescription = stateText }
            .background(fill, RectangleShape)
            .border(3.dp, accent, RectangleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(accent, RectangleShape), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (visual == OptionVisual.NEUTRAL || visual == OptionVisual.DIMMED) cs.surface else cs.background,
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = if (visual == OptionVisual.DIMMED) cs.onSurface.copy(alpha = 0.55f) else cs.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
        when (visual) {
            OptionVisual.CORRECT -> Icon(Icons.Filled.CheckCircle, null, tint = CorrectColor)
            OptionVisual.WRONG -> Icon(Icons.Filled.Cancel, null, tint = WrongColor)
            else -> Unit
        }
    }
}

@Composable
private fun FeedbackBanner(feedback: AnswerFeedback) {
    val (color, textRes) = when {
        feedback.timedOut -> WrongColor to R.string.game_timeout
        feedback.isCorrect && feedback.multiplierApplied -> CorrectColor to R.string.game_correct_x2
        feedback.isCorrect -> CorrectColor to R.string.game_correct
        else -> WrongColor to R.string.game_wrong
    }
    // Text uses the normal on-surface colour (always high contrast); the meaning colour is the tint and border.
    Text(
        text = if (feedback.isCorrect) stringResource(textRes, feedback.earnedPoints) else stringResource(textRes),
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.14f), RectangleShape)
            .border(3.dp, color, RectangleShape)
            .padding(12.dp),
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
    )
}

@Composable
private fun categoryLabel(category: QuestionCategory): String = stringResource(
    when (category) {
        QuestionCategory.GENERAL -> R.string.category_general
        QuestionCategory.TOKENS -> R.string.category_tokens
        QuestionCategory.AI_TOOLS -> R.string.category_ai_tools
        QuestionCategory.MODELS -> R.string.category_models
        QuestionCategory.ARCHITECTURE -> R.string.category_architecture
    },
)
