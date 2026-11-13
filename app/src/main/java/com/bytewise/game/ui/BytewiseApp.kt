package com.bytewise.game.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.data.model.ThemeMode
import com.bytewise.game.ui.navigation.BytewiseNavHost
import com.bytewise.game.ui.theme.BytewiseTheme
import com.bytewise.game.utils.LocalizedContent
import com.bytewise.game.viewmodel.ProgressViewModel
import com.bytewise.game.viewmodel.SettingsViewModel

@Composable
fun BytewiseApp(onQuit: () -> Unit, viewModel: SettingsViewModel = viewModel(),
    progressViewModel: ProgressViewModel = viewModel(),
) {
    val byteyTier by progressViewModel.byteyTier.collectAsStateWithLifecycle()
    val progress by progressViewModel.progress.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LocalizedContent(settings.language) {
        BytewiseTheme(darkTheme = dark) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BytewiseNavHost(settings = settings, isDark = dark, byteyTier = byteyTier, progress = progress, viewModel = viewModel, onQuit = onQuit)
            }
        }
    }
}
