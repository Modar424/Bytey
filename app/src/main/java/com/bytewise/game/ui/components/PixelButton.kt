package com.bytewise.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.ui.theme.PixelFontFamily

/** Chunky block button with a hard offset shadow (no rounded corners) for the pixel-game look. */
@Composable
fun PixelButton(
    text: String,
    icon: ImageVector,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 54,
) {
    val edge = lerp(container, Color.Black, 0.45f)
    Box(
        modifier
            .height((height + 5).dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .padding(top = 5.dp)
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.55f), RectangleShape),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .align(Alignment.TopStart)
                .background(container, RectangleShape)
                .border(3.dp, edge, RectangleShape)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(22.dp))
            Box(Modifier.size(8.dp))
            Text(
                text = text,
                color = onContainer,
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
            )
        }
    }
}

/** Small square icon control used in the top bar of the main menu. */
@Composable
fun PixelIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val tint = if (active) Color(0xFFFBBF24) else Color(0xFF94A3B8)
    Box(
        modifier
            .size(46.dp)
            .background(Color(0xCC0F172A), RoundedCornerShape(4.dp))
            .border(3.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
    }
}
