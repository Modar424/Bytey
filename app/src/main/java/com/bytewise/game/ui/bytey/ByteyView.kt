package com.bytewise.game.ui.bytey

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Bytey + speech bubble in the display mode chosen by the controller (normal / interaction / celebration). */
@Composable
fun ByteyView(
    controller: ByteyController,
    tier: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val spriteModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
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
