package com.bytewise.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.QuestionCategory
import com.bytewise.game.data.model.explanation
import com.bytewise.game.data.model.options
import com.bytewise.game.data.model.text
import com.bytewise.game.domain.SoundCue
import com.bytewise.game.domain.TickPolicy
import com.bytewise.game.domain.TimerZone
import com.bytewise.game.ui.bytey.ByteyController
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteySprites
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.components.Entrance
import com.bytewise.game.ui.components.PixelButton
import com.bytewise.game.ui.components.StatPill
import com.bytewise.game.ui.components.isLandscape
import com.bytewise.game.ui.theme.Brand
import com.bytewise.game.ui.theme.GoldFill
import com.bytewise.game.ui.theme.LocalStatusColors
import com.bytewise.game.ui.theme.OnGoldFill
import com.bytewise.game.ui.theme.PixelFontFamily
import com.bytewise.game.viewmodel.AnswerFeedback
import com.bytewise.game.viewmodel.GameError
import com.bytewise.game.viewmodel.GameNav
import com.bytewise.game.viewmodel.GamePhase
import com.bytewise.game.viewmodel.GameUiState
import com.bytewise.game.viewmodel.GameViewModel
import com.bytewise.game.viewmodel.QuestionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val sounds = remember { (context.applicationContext as BytewiseApplication).container.soundManager }
    val resources = LocalContext.current.resources
    val latestOnFinished by rememberUpdatedState(onFinished)

    // No sound may outlive the gameplay screen.
    DisposableEffect(Unit) { onDispose { sounds.stopAll() } }

    // Leaving the app freezes the timer and silences everything; coming back continues with the time that was left.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.setLifecyclePaused(true)
        sounds.stopAll()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.setLifecyclePaused(false) }

    // Colour flash for fresh answers (not for revisits): green for right, red for wrong / timeout.
    val flash = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }
    val flashScope = rememberCoroutineScope()
    val goodColor = CorrectColor
    val badColor = WrongColor

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            // The controller plays the reaction's sounds in the same instant it shows the animation and the bubble.
            when (event) {
                ByteyEvent.CORRECT, ByteyEvent.FAST_CORRECT, ByteyEvent.STREAK -> flashColor = goodColor
                ByteyEvent.INCORRECT, ByteyEvent.TIMEOUT -> flashColor = badColor
                else -> flashColor = Color.Transparent
            }
            if (flashColor != Color.Transparent) {
                // Own coroutine: the flash must not delay Bytey's reaction (sound + animation + bubble).
                flashScope.launch {
                    flash.snapTo(0.32f)
                    flash.animateTo(0f, tween(650))
                }
            }
            bytey.react(event, resources)
        }
    }
    LaunchedEffect(viewModel) { viewModel.navigation.collect { latestOnFinished(it) } }

    // A soft tick for each of the last five seconds (game-sound toggle applies inside the SoundManager).
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
                    sounds.play(SoundCue.TICK)
                }
            }
        }
    }

    BackHandler(enabled = state.phase == GamePhase.PLAYING || state.phase == GamePhase.TIMER_SELECT) {
        when {
            state.phase == GamePhase.TIMER_SELECT -> onExit()
            state.exitDialogVisible -> viewModel.hideExitDialog()
            else -> viewModel.showExitDialog()
        }
    }

    Box(Modifier.fillMaxSize().background(Brand.Sky)) {
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
            GamePhase.TIMER_SELECT -> TimerSelectContent(
                level = state.level,
                selected = state.pendingTimerSeconds,
                onSelect = viewModel::selectTimer,
                onStart = viewModel::confirmTimer,
                onBack = onExit,
                bytey = bytey,
                byteyTier = byteyTier,
            )
            GamePhase.PLAYING -> PlayingContent(
                state = state,
                language = settings.language,
                remaining = viewModel.remainingMs,
                bytey = bytey,
                byteyTier = byteyTier,
                onClose = viewModel::showExitDialog,
                onAnswer = viewModel::submitAnswer,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
            )
        }
        // Answer flash on top of everything, never intercepting touches.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = flash.value }
                .background(Brush.radialGradient(listOf(Color.Transparent, flashColor))),
        )
    }

    if (state.exitDialogVisible && state.phase == GamePhase.PLAYING) {
        AlertDialog(
            onDismissRequest = viewModel::hideExitDialog,
            title = { Text(stringResource(R.string.game_exit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Bytey's remark is also written inside the dialog, because the dialog dims him behind it.
                    bytey.message?.let { Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }
                    Text(stringResource(R.string.game_exit_message))
                }
            },
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
    onPrevious: () -> Unit,
) {
    val question = state.question ?: return
    val feedback = state.feedback
    val cs = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    val labels = stringArrayResource(R.array.option_labels)
    val last = state.index + 1 >= state.total

    LaunchedEffect(state.index) { scroll.scrollTo(0) }
    LaunchedEffect(state.index, feedback != null) {
        if (feedback != null) {
            delay(250)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // ---- top bar: close, level, score, streak
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
                    fontFamily = PixelFontFamily,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    stringResource(R.string.game_question_counter, state.index + 1, state.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            val scoreDescription = stringResource(R.string.cd_score, state.score)
            StatPill(Modifier.semantics { contentDescription = scoreDescription }) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = WarnColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AnimatedNumber(state.score)
            }
            Spacer(Modifier.width(8.dp))
            val streakDescription = stringResource(R.string.cd_streak, state.streak)
            StatPill(Modifier.semantics { contentDescription = streakDescription }) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = FireColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                AnimatedNumber(state.streak)
            }
            Spacer(Modifier.width(8.dp))
        }

        // ---- progress strip + timer ring + multiplier
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressStrip(state.statuses, state.index)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TimerRing(remaining, state.timerSeconds)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        categoryLabel(question.category),
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.secondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    AnimatedVisibility(visible = state.multiplierActive) {
                        Text(
                            stringResource(if (feedback == null) R.string.game_multiplier_active else R.string.game_multiplier_next),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(Brush.horizontalGradient(listOf(GoldFill, Brand.GoldDeep)), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            color = OnGoldFill,
                            fontFamily = PixelFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        val questionBlock: @Composable ColumnScope.() -> Unit = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(cs.surfaceVariant, cs.surface)),
                        RoundedCornerShape(22.dp),
                    )
                    .border(2.dp, cs.outlineVariant, RoundedCornerShape(22.dp))
                    .padding(18.dp),
            ) {
                Text(
                    question.text(language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                )
            }
        }
        val optionsBlock: @Composable ColumnScope.() -> Unit = {
            question.options(language).forEachIndexed { i, option ->
                val visual = when {
                    feedback == null -> OptionVisual.NEUTRAL
                    i == question.correctIndex -> OptionVisual.CORRECT
                    i == feedback.selectedIndex -> OptionVisual.WRONG
                    state.canChangeAnswer -> OptionVisual.NEUTRAL // still tappable: do not dim it
                    else -> OptionVisual.DIMMED
                }
                Entrance(delayMs = 70 + i * 55) {
                    OptionCard(
                        label = labels.getOrElse(i) { (i + 1).toString() },
                        text = option,
                        visual = visual,
                        enabled = feedback == null || state.canChangeAnswer,
                        onClick = { onAnswer(i) },
                    )
                }
            }
        }
        val feedbackBlock: @Composable ColumnScope.() -> Unit = {
            AnimatedVisibility(
                visible = feedback != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                exit = fadeOut(tween(120)),
            ) {
                if (feedback != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeedbackBanner(feedback)
                        if (state.canChangeAnswer) {
                            Text(
                                stringResource(R.string.game_change_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        } else if (feedback.timedOut) {
                            Text(
                                stringResource(R.string.game_locked_expired),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(cs.surface, RoundedCornerShape(18.dp))
                                .border(2.dp, cs.outlineVariant, RoundedCornerShape(18.dp))
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
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            // key(index): the whole question re-enters with a fade + slide whenever the player moves to another one.
            key(state.index) {
                if (isLandscape()) {
                    // Wide and short: the question and its explanation on the left, the four options on the right.
                    val rightScroll = rememberScrollState()
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            Modifier.weight(1f).fillMaxHeight().verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Entrance { Column { questionBlock() } }
                            feedbackBlock()
                            Spacer(Modifier.height(130.dp))
                        }
                        Column(
                            Modifier.weight(1f).fillMaxHeight().verticalScroll(rightScroll),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) { optionsBlock() }
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Entrance { Column { questionBlock() } }
                        optionsBlock()
                        feedbackBlock()
                        // Room so the last card can scroll above Bytey in the corner.
                        Spacer(Modifier.height(130.dp))
                    }
                }
            }
            ByteyView(
                bytey,
                byteyTier,
                Modifier.align(if (isLandscape()) Alignment.BottomStart else Alignment.BottomEnd).padding(8.dp),
            )
        }

        // ---- bottom bar: previous / next
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PixelButton(
                text = stringResource(R.string.game_previous),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                container = cs.surfaceVariant,
                onContainer = cs.onSurface,
                onClick = onPrevious,
                enabled = state.canGoPrevious,
                modifier = Modifier.weight(0.9f),
            )
            PixelButton(
                text = stringResource(if (last) R.string.game_finish else R.string.game_next),
                icon = if (last) Icons.Filled.EmojiEvents else Icons.AutoMirrored.Filled.ArrowForward,
                container = if (last) GoldFill else cs.primary,
                onContainer = if (last) OnGoldFill else cs.onPrimary,
                onClick = onNext,
                enabled = state.canGoNext,
                modifier = Modifier.weight(1.1f),
            )
        }
    }
}

/** One segment per question: green / red / grey-red once resolved, the current one outlined. */
@Composable
private fun ProgressStrip(statuses: List<QuestionStatus>, current: Int) {
    if (statuses.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        statuses.forEachIndexed { i, status ->
            val target = when (status) {
                QuestionStatus.CORRECT -> CorrectColor
                QuestionStatus.WRONG -> WrongColor
                QuestionStatus.EXPIRED -> WarnColor
                QuestionStatus.PENDING -> if (i == current) cs.primary else cs.surfaceVariant
            }
            val color by animateColorAsState(target, tween(250), label = "segment")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
                    .then(if (i == current) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(3.dp)) else Modifier),
            )
        }
    }
}

@Composable
private fun AnimatedNumber(value: Int) {
    AnimatedContent(
        targetState = value,
        transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
        label = "number",
    ) { v ->
        Text(v.toString(), fontFamily = PixelFontFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

/** Circular countdown for the selected duration; colour follows the 50 % / 25 % zones and pulses in the red zone. */
@Composable
private fun TimerRing(remainingFlow: StateFlow<Long>, totalSeconds: Int) {
    val remaining by remainingFlow.collectAsStateWithLifecycle()
    val fraction = (remaining.toFloat() / (totalSeconds * 1000f)).coerceIn(0f, 1f)
    val zone = TimerZone.of(fraction)
    val color = when (zone) {
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
    val urgent = zone == TimerZone.RED && remaining > 0 && !reduceMotion
    val description = stringResource(R.string.cd_timer_remaining, seconds)
    Box(
        Modifier.size(64.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
        )
        Text(
            seconds.toString(),
            modifier = Modifier.graphicsLayer { alpha = if (urgent) pulse.value else 1f },
            color = color,
            fontFamily = PixelFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
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
        else -> cs.outlineVariant
    }
    val targetFill = when (visual) {
        OptionVisual.CORRECT -> CorrectColor.copy(alpha = 0.2f)
        OptionVisual.WRONG -> WrongColor.copy(alpha = 0.2f)
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
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shake.value
            }
            .semantics(mergeDescendants = true) { if (stateText != null) stateDescription = stateText }
            .background(fill, shape)
            .border(2.dp, accent, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(
                if (visual == OptionVisual.NEUTRAL || visual == OptionVisual.DIMMED) cs.primary else accent,
                RoundedCornerShape(11.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color = if (visual == OptionVisual.NEUTRAL || visual == OptionVisual.DIMMED) cs.onPrimary else cs.background,
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
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .border(2.dp, color, RoundedCornerShape(16.dp))
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
        QuestionCategory.SCENARIOS -> R.string.category_scenarios
    },
)
