package com.bytewise.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bytewise.game.ui.components.GameScaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    byteyTier: Int,
    onBack: () -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onSound: (Boolean) -> Unit,
    onByteyMuted: (Boolean) -> Unit,
) {
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    LaunchedEffect(settings.language) { bytey.react(ByteyEvent.SETTINGS_TIP, resources) }
    GameScaffold(title = stringResource(R.string.settings_title), onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(stringResource(R.string.settings_language)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.language == AppLanguage.AR,
                        onClick = { onLanguage(AppLanguage.AR) },
                        label = { Text(stringResource(R.string.language_arabic)) },
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.EN,
                        onClick = { onLanguage(AppLanguage.EN) },
                        label = { Text(stringResource(R.string.language_english)) },
                    )
                }
            }
            SwitchRow(
                title = stringResource(R.string.settings_sound),
                description = null,
                checked = settings.soundEnabled,
                onChecked = onSound,
            )
            SwitchRow(
                title = stringResource(R.string.settings_bytey_sound),
                description = stringResource(R.string.settings_bytey_sound_desc),
                checked = !settings.byteyMuted,
                onChecked = { on -> onByteyMuted(!on) },
            )
        }
        ByteyView(bytey, byteyTier, Modifier.align(Alignment.BottomEnd).padding(16.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
