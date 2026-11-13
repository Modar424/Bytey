package com.bytewise.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.ThemeMode
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
    onTheme: (ThemeMode) -> Unit,
    onSound: (Boolean) -> Unit,
    onByteyMuted: (Boolean) -> Unit,
) {
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    LaunchedEffect(settings.language) { bytey.react(ByteyEvent.SETTINGS_TIP, resources) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
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
            Section(stringResource(R.string.settings_theme)) {
                ThemeRow(R.string.theme_system, settings.themeMode == ThemeMode.SYSTEM) { onTheme(ThemeMode.SYSTEM) }
                ThemeRow(R.string.theme_light, settings.themeMode == ThemeMode.LIGHT) { onTheme(ThemeMode.LIGHT) }
                ThemeRow(R.string.theme_dark, settings.themeMode == ThemeMode.DARK) { onTheme(ThemeMode.DARK) }
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
private fun ThemeRow(labelRes: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(labelRes), Modifier.padding(start = 12.dp))
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
