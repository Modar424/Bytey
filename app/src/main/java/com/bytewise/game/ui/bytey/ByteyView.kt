package com.bytewise.game.ui.bytey

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Bytey + speech bubble in the display mode chosen by the controller (normal / interaction / celebration).
 *
 * Bytey is a character, not a picture: he gently bobs while idle, hops when tapped and answers
 * with a voice (squeak / roar / giggle) plus a playful remark. Pass [onClick] only to override the default tap.
 */
@Composable
fun ByteyView(
    controller: ByteyController,
    tier: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val resources = context.resources
    val scope = rememberCoroutineScope()
    var tapJob by remember { mutableStateOf<Job?>(null) }
    val reduceMotion = remember { ByteySprites.isReducedMotion(context) }

    // Idle bob (skipped when system animations are off) and a hop on every tap.
    val bob = rememberInfiniteTransition(label = "byteyBob").animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "byteyBobY",
    )
    val squash = remember { Animatable(0f) }
    LaunchedEffect(controller.tapCount) {
        if (controller.tapCount == 0 || reduceMotion) return@LaunchedEffect
        // A hop (no scaling): the pixel art stays razor sharp and keeps exactly its original look.
        squash.snapTo(0f)
        squash.animateTo(-14f, tween(110, easing = FastOutSlowInEasing))
        squash.animateTo(0f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
    }

    val spriteModifier = Modifier
        .graphicsLayer {
            translationY = if (reduceMotion) 0f else (bob.value + squash.value).dp.toPx()
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClick = {
                if (onClick != null) {
                    onClick()
                } else {
                    tapJob?.cancel()
                    tapJob = scope.launch { controller.tap(resources) }
                }
            },
        )
    if (controller.mode == ByteyDisplayMode.CELEBRATION) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            ByteySpeechBubble(controller.message)
            ByteySprite(controller.state, tier, controller.mode.size, spriteModifier)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.Bottom) {
            ByteySpeechBubble(controller.message, Modifier.padding(bottom = 36.dp))
            ByteySprite(controller.state, tier, controller.mode.size, spriteModifier)
        }
    }
}
