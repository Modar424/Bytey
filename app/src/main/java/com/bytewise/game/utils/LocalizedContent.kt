package com.bytewise.game.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.bytewise.game.data.model.AppLanguage
import java.util.Locale

/**
 * Applies the selected language and layout direction to everything inside [content]
 * immediately, without recreating the Activity.
 */
@Composable
fun LocalizedContent(language: AppLanguage, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val config = remember(language, base) {
        val locale = Locale(language.tag)
        Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localized = remember(config, base) { base.createConfigurationContext(config) }
    val direction = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides config,
        LocalLayoutDirection provides direction,
        content = content,
    )
}
