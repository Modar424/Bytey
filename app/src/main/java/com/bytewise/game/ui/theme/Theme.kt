package com.bytewise.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.bytewise.game.R

/**
 * Bundled pixel font: Silkscreen (SIL Open Font License, see licenses/Silkscreen-OFL.txt). It covers Latin only;
 * Arabic glyphs fall back to the system font automatically.
 */
val PixelFontFamily: FontFamily = FontFamily(Font(R.font.silkscreen_regular))

private val Gold = Color(0xFFFBBF24)

/** Fixed gold used as a FILL (dark text on it has 9.6:1 contrast in both themes). */
val GoldFill = Gold
/** Dark text colour for use on [GoldFill]. */
val OnGoldFill = Color(0xFF1E1B4B)

/**
 * Meaning colours (right / wrong / warning / streak). Separate light and dark values so every one keeps
 * >= 4.5:1 (text) or >= 3:1 (icons, bars, borders) against the theme's surfaces; checked with the WCAG formula.
 * [goodText]/[badText] are for small text on a tinted background.
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
private val LightStatus = StatusColors(
    good = Color(0xFF15803D), bad = Color(0xFFB91C1C), warn = Color(0xFFB45309), fire = Color(0xFFC2410C),
    goodText = Color(0xFF166534), badText = Color(0xFF991B1B),
)

val LocalStatusColors = staticCompositionLocalOf { DarkStatus }

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1B4B),
    tertiary = Gold,
    onTertiary = Color(0xFF1E1B4B),
    background = Color(0xFF0F172A),
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF8091AB),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    tertiary = Color(0xFFD99A00),
    onTertiary = Color(0xFF1E1B4B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
)

@Composable
fun BytewiseTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides (if (darkTheme) DarkStatus else LightStatus)) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
