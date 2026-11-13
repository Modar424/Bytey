package com.bytewise.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.Concept
import com.bytewise.game.data.model.ResourceLanguage
import com.bytewise.game.data.model.advancedChallenge
import com.bytewise.game.data.model.name
import com.bytewise.game.data.model.practiceChallenge
import com.bytewise.game.data.model.title
import com.bytewise.game.domain.Advice
import com.bytewise.game.domain.AdviceKind
import com.bytewise.game.domain.Trend
import com.bytewise.game.ui.theme.LocalStatusColors
import com.bytewise.game.ui.theme.PixelFontFamily


@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        fontFamily = PixelFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Square-cornered bordered block used for every report card. */
@Composable
fun PixelCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .background(cs.surface, RectangleShape)
            .border(3.dp, cs.outline, RectangleShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = PixelFontFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

@Composable
fun AccuracyBar(accuracy: Float, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    val color = when {
        accuracy >= 0.8f -> status.good
        accuracy >= 0.5f -> status.warn
        else -> status.bad
    }
    Box(modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)) {
        Box(Modifier.fillMaxWidth(accuracy.coerceIn(0f, 1f)).height(10.dp).background(color, RectangleShape))
    }
}

@Composable
fun Badge(text: String, color: Color, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    // Tinted chip with a readable (on-surface or dedicated "text" variant) label.
    Text(
        text,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RectangleShape)
            .border(2.dp, color, RectangleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = textColor,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
}

@Composable
fun TrendBadge(trend: Trend) {
    when (trend) {
        Trend.IMPROVING -> Badge(stringResource(R.string.trend_improving), LocalStatusColors.current.good, LocalStatusColors.current.goodText)
        Trend.REGRESSING -> Badge(stringResource(R.string.trend_regressing), LocalStatusColors.current.bad, LocalStatusColors.current.badText)
        Trend.STABLE -> Badge(stringResource(R.string.trend_stable), MaterialTheme.colorScheme.onSurfaceVariant)
        Trend.NEW -> Unit // no history yet: nothing worth saying
    }
}

@Composable
fun adviceText(advice: Advice, nameOf: (String) -> String): String {
    val name = nameOf(advice.conceptId.orEmpty())
    return when (advice.kind) {
        AdviceKind.PERFECT -> stringResource(R.string.advice_perfect)
        AdviceKind.FOCUS_CONCEPT -> stringResource(R.string.advice_focus, name)
        AdviceKind.PERSISTENT_CONCEPT -> stringResource(R.string.advice_persistent, name)
        AdviceKind.IMPROVED_CONCEPT -> stringResource(R.string.advice_improved, name)
        AdviceKind.REGRESSED_CONCEPT -> stringResource(R.string.advice_regressed, name)
        AdviceKind.TIMEOUTS -> stringResource(R.string.advice_timeouts, advice.count)
        AdviceKind.SLOW_DOWN -> stringResource(R.string.advice_slow_down)
        AdviceKind.SPEED_UP -> stringResource(R.string.advice_speed_up)
        AdviceKind.RETRY_FAILED -> stringResource(R.string.advice_retry)
        AdviceKind.NEXT_LEVEL -> stringResource(R.string.advice_next)
    }
}

/**
 * One concept with its accuracy, badges and (when [showDetails]) the practice challenge and
 * up to three learning resources. Tapping a resource opens it in the browser.
 */
@Composable
fun ConceptCard(
    concept: Concept?,
    language: AppLanguage,
    attempts: Int,
    correct: Int,
    trend: Trend,
    persistent: Boolean,
    weakLevels: List<Int>,
    showDetails: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    PixelCard {
        Text(
            concept?.name(language) ?: stringResource(R.string.report_unknown_concept),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
        )
        val accuracy = if (attempts == 0) 0f else correct.toFloat() / attempts
        AccuracyBar(accuracy)
        Text(
            stringResource(R.string.report_accuracy, (accuracy * 100).toInt(), correct, attempts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TrendBadge(trend)
            if (persistent) Badge(stringResource(R.string.badge_persistent), LocalStatusColors.current.bad, LocalStatusColors.current.badText)
        }
        if (weakLevels.isNotEmpty()) {
            Text(
                stringResource(R.string.report_weak_levels, weakLevels.joinToString(if (language == AppLanguage.AR) "، " else ", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showDetails && concept != null) {
            Text(stringResource(R.string.report_practice), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(concept.practiceChallenge(language), style = MaterialTheme.typography.bodyMedium)
            concept.advancedChallenge(language)?.let {
                Text(stringResource(R.string.report_advanced), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            val resources = concept.resources.take(3)
            if (resources.isNotEmpty()) {
                Text(stringResource(R.string.report_resources), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                resources.forEach { r ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { runCatching { uriHandler.openUri(r.url) } }
                            .padding(vertical = 4.dp),
                    ) {
                        Text(r.title(language), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(
                                if (r.language == ResourceLanguage.AR) R.string.report_resource_lang_ar else R.string.report_resource_lang_en,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
