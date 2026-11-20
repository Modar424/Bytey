package com.bytewise.game.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.R
import com.bytewise.game.domain.GameConfig
import com.bytewise.game.ui.bytey.ByteyController
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.components.Entrance
import com.bytewise.game.ui.components.GameScaffold
import com.bytewise.game.ui.components.PixelButton
import com.bytewise.game.ui.theme.Brand
import com.bytewise.game.ui.theme.GoldFill
import com.bytewise.game.ui.theme.OnGoldFill
import com.bytewise.game.ui.theme.PixelFontFamily

/**
 * Shown before every level: pick 20-90 seconds per question (20 s highlighted by default). The choice applies to
 * the whole level and is not asked again between questions.
 */
@Composable
fun TimerSelectContent(
    level: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    bytey: ByteyController,
    byteyTier: Int,
) {
    val resources = LocalContext.current.resources
    LaunchedEffect(level) { bytey.react(ByteyEvent.LEVELS_TIP, resources) }
    GameScaffold(title = stringResource(R.string.timer_title), onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.timer_subtitle, level),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                GameConfig.TIMER_OPTIONS_SECONDS.chunked(4).forEachIndexed { rowIndex, rowOptions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowOptions.forEachIndexed { col, seconds ->
                            Entrance(delayMs = (rowIndex * 4 + col) * 40, modifier = Modifier.weight(1f)) {
                                TimerChip(
                                    seconds = seconds,
                                    selected = seconds == selected,
                                    isDefault = seconds == GameConfig.QUESTION_TIME_SECONDS,
                                    onClick = { onSelect(seconds) },
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.timer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ByteyView(bytey, byteyTier)
                }
            }
            PixelButton(
                text = stringResource(R.string.timer_start),
                icon = Icons.Filled.PlayArrow,
                container = GoldFill,
                onContainer = OnGoldFill,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                height = 58,
            )
        }
    }
}

@Composable
private fun TimerChip(seconds: Int, selected: Boolean, isDefault: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipScale",
    )
    val border by animateColorAsState(if (selected) Brand.Gold else cs.outlineVariant, tween(160), label = "chipBorder")
    val shape = RoundedCornerShape(18.dp)
    val description = stringResource(if (selected) R.string.timer_option_selected_cd else R.string.timer_option_cd, seconds)
    Box(Modifier.fillMaxWidth().scale(scale)) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .background(
                    if (selected) Brush.verticalGradient(listOf(Brand.Violet, Brand.VioletDeep))
                    else Brush.verticalGradient(listOf(cs.surfaceVariant, cs.surface)),
                    shape,
                )
                .border(if (selected) 3.dp else 2.dp, border, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                seconds.toString(),
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = if (selected) Color.White else cs.onSurface,
            )
            Text(
                stringResource(R.string.timer_unit),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else cs.onSurfaceVariant,
            )
        }
        if (isDefault) {
            Text(
                stringResource(R.string.timer_default_badge),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-9).dp)
                    .background(Brand.Gold, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = OnGoldFill,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
