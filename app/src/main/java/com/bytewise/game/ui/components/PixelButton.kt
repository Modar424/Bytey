package com.bytewise.game.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.ui.theme.PixelFontFamily

private val ButtonShape = RoundedCornerShape(16.dp)
private val SlabDepth = 5.dp

/**
 * Chunky "3D" game button: glossy gradient face on a darker slab that the face sinks into when pressed.
 *
 * The label gets the remaining width and may wrap to a second line, so long texts such as "My Progress" are
 * never clipped (the old fixed-height, single-line button cut the label at the first word on narrow screens).
 * [height] is the minimum face height; the button grows if the text needs more room.
 */
@Composable
fun PixelButton(
    text: String,
    icon: ImageVector,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 54,
    enabled: Boolean = true,
) {
    val edge = lerp(container, Color.Black, 0.5f)
    val gloss = lerp(container, Color.White, 0.22f)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed && enabled) 3.dp else 0.dp, tween(80), label = "buttonSink")
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Box(Modifier.matchParentSize().padding(top = SlabDepth).background(edge, ButtonShape))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = SlabDepth)
                .offset(y = sink)
                .heightIn(min = height.dp)
                .background(Brush.verticalGradient(listOf(gloss, container)), ButtonShape)
                .border(2.dp, lerp(container, Color.Black, 0.25f), ButtonShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                color = onContainer,
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Small round icon control (sound, language, Bytey voice) used in the top bar of the main menu. */
@Composable
fun PixelIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val tint = if (active) Color(0xFFFFC53D) else Color(0xFF9AA6D6)
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .size(46.dp)
            .background(Color(0xCC151C3A), shape)
            .border(2.dp, if (active) Color(0xFF6E7BD6) else Color(0xFF39437A), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}
