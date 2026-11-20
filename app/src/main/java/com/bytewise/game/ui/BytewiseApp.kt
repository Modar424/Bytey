package com.bytewise.game.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bytewise.game.ui.navigation.BytewiseNavHost
import com.bytewise.game.ui.theme.BytewiseTheme
import com.bytewise.game.utils.LocalizedContent
import com.bytewise.game.viewmodel.ProgressViewModel
import com.bytewise.game.viewmodel.SettingsViewModel

/** The app is dark-only: there is no theme setting and no system/light handling anywhere. */
@Composable
fun BytewiseApp(onQuit: () -> Unit, viewModel: SettingsViewModel = viewModel(),
    progressViewModel: ProgressViewModel = viewModel(),
) {
    val byteyTier by progressViewModel.byteyTier.collectAsStateWithLifecycle()
    val progress by progressViewModel.progress.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    LocalizedContent(settings.language) {
        BytewiseTheme {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                BytewiseNavHost(settings = settings, byteyTier = byteyTier, progress = progress, viewModel = viewModel, onQuit = onQuit)
            }
        }
    }
}
