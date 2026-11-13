package com.bytewise.game.ui.bytey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.ui.theme.PixelFontFamily

/**
 * Pixel-styled speech bubble: square corners, hard border, stepped tail pointing to Bytey
 * (the tail sits at the layout-end side, so it flips automatically in RTL).
 * Pass null to hide it; it fades/scales in and out.
 */
@Composable
fun ByteySpeechBubble(text: String?, modifier: Modifier = Modifier) {
    val last = remember { arrayOf("") }
    if (text != null) last[0] = text
    val cs = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.85f),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Box(
                Modifier
                    .widthIn(max = 168.dp)
                    .background(cs.surface, RectangleShape)
                    .border(3.dp, cs.onSurface, RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = last[0],
                    color = cs.onSurface,
                    fontFamily = PixelFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Box(Modifier.padding(end = 14.dp).width(12.dp).height(4.dp).background(cs.onSurface))
            Box(Modifier.padding(end = 18.dp).width(8.dp).height(4.dp).background(cs.onSurface))
            Box(Modifier.padding(end = 22.dp).size(4.dp).background(cs.onSurface))
        }
    }
}
