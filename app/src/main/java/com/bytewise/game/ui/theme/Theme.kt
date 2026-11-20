package com.bytewise.game.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bytewise.game.R

/**
 * Bundled pixel font: Silkscreen (SIL Open Font License, see licenses/Silkscreen-OFL.txt). It covers Latin only;
 * Arabic glyphs fall back to the system font automatically. It is kept for titles, numbers and badges as the
 * brand voice; body text uses the system font for readability in both languages.
 */
val PixelFontFamily: FontFamily = FontFamily(Font(R.font.silkscreen_regular))

private val Gold = Color(0xFFFFC53D)

/** Fixed gold used as a FILL (dark text on it has > 9:1 contrast). */
val GoldFill = Gold
/** Dark text colour for use on [GoldFill]. */
val OnGoldFill = Color(0xFF1B1440)

/** Brand colours used by the redesigned screens (the app is dark-only, so these are plain constants). */
object Brand {
    val Ink = Color(0xFF0A0F24)
    val InkDeep = Color(0xFF060918)
    val Panel = Color(0xFF151C3A)
    val PanelHigh = Color(0xFF1F2850)
    val Violet = Color(0xFF7C8CFF)
    val VioletDeep = Color(0xFF4B3FD1)
    val Teal = Color(0xFF2DD4BF)
    val TealDeep = Color(0xFF0F9F8F)
    val Gold = Color(0xFFFFC53D)
    val GoldDeep = Color(0xFFE08A00)
    val Coral = Color(0xFFFF6B7A)

    /** Screen background: a calm vertical night-sky gradient. */
    val Sky: Brush = Brush.verticalGradient(listOf(Color(0xFF141B45), Ink, InkDeep))
}

/**
 * Meaning colours (right / wrong / warning / streak), tuned for the dark surfaces: every one keeps >= 4.5:1 for text
 * and >= 3:1 for icons, bars and borders (WCAG formula). [goodText]/[badText] are for small text on tinted backgrounds.
 */
@Immutable
data class StatusColors(
    val good: Color,
    val bad: Color,
    val warn: Color,
    val fire: Color,
    val goodText: Color,
    val badText: Color,
)

private val DarkStatus = StatusColors(
    good = Color(0xFF22C55E), bad = Color(0xFFEF4444), warn = Color(0xFFFBBF24), fire = Color(0xFFF97316),
    goodText = Color(0xFF86EFAC), badText = Color(0xFFFCA5A5),
)

val LocalStatusColors = staticCompositionLocalOf { DarkStatus }

private val DarkColors = darkColorScheme(
    primary = Brand.Violet,
    onPrimary = Color(0xFF12103A),
    primaryContainer = Brand.VioletDeep,
    onPrimaryContainer = Color.White,
    secondary = Brand.Teal,
    onSecondary = Color(0xFF042F2B),
    tertiary = Gold,
    onTertiary = OnGoldFill,
    background = Brand.Ink,
    onBackground = Color.White,
    surface = Brand.Panel,
    onSurface = Color.White,
    surfaceVariant = Brand.PanelHigh,
    onSurfaceVariant = Color(0xFFCBD5F5),
    outline = Color(0xFF7E8BC4),
    outlineVariant = Color(0xFF39437A),
    error = Brand.Coral,
    onError = Color(0xFF3B0610),
)

private val GameShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Dark mode only. There is intentionally no light / system variant. */
@Composable
fun BytewiseTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides DarkStatus) {
        MaterialTheme(
            colorScheme = DarkColors,
            shapes = GameShapes,
            content = content,
        )
    }
}
