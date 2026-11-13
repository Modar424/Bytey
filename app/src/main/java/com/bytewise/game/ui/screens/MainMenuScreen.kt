package com.bytewise.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bytewise.game.R
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController
import com.bytewise.game.ui.components.GameTitle
import com.bytewise.game.ui.components.MenuBackdrop
import com.bytewise.game.ui.components.PixelButton
import com.bytewise.game.ui.components.PixelIconButton
import com.bytewise.game.ui.components.isLandscape

@Composable
fun MainMenuScreen(
    settings: AppSettings,
    isDark: Boolean,
    byteyTier: Int,
    onStart: () -> Unit,
    onLevels: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleByteyMute: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    var taps by remember { mutableIntStateOf(0) }
    LaunchedEffect(settings.language, taps) { bytey.react(ByteyEvent.MENU_GREETING, resources) }
    val landscape = isLandscape()
    val controls: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelIconButton(
                icon = if (settings.soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(if (settings.soundEnabled) R.string.cd_sound_on else R.string.cd_sound_off),
                onClick = onToggleSound,
                active = settings.soundEnabled,
            )
            PixelIconButton(
                icon = Icons.Filled.Language,
                contentDescription = stringResource(R.string.cd_language),
                onClick = onToggleLanguage,
            )
            PixelIconButton(
                icon = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = stringResource(R.string.cd_theme),
                onClick = onToggleTheme,
            )
            PixelIconButton(
                icon = if (settings.byteyMuted) Icons.Filled.VoiceOverOff else Icons.Filled.RecordVoiceOver,
                contentDescription = stringResource(if (settings.byteyMuted) R.string.cd_bytey_muted else R.string.cd_bytey_unmuted),
                onClick = onToggleByteyMute,
                active = !settings.byteyMuted,
            )
        }
    }
    val mascot: @Composable () -> Unit = { ByteyView(bytey, byteyTier, onClick = { taps++ }) }
    val buttons: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            PixelButton(
                text = stringResource(R.string.menu_start),
                icon = Icons.Filled.PlayArrow,
                container = cs.tertiary,
                onContainer = cs.onTertiary,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                height = 58,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelButton(
                    text = stringResource(R.string.menu_levels),
                    icon = Icons.Filled.GridView,
                    container = cs.primary,
                    onContainer = cs.onPrimary,
                    onClick = onLevels,
                    modifier = Modifier.weight(1f),
                    height = 48,
                )
                PixelButton(
                    text = stringResource(R.string.menu_progress),
                    icon = Icons.Filled.BarChart,
                    container = cs.secondary,
                    onContainer = cs.onSecondary,
                    onClick = onProgress,
                    modifier = Modifier.weight(1f),
                    height = 48,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelButton(
                    text = stringResource(R.string.menu_settings),
                    icon = Icons.Filled.Settings,
                    container = cs.surfaceVariant,
                    onContainer = cs.onSurfaceVariant,
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    height = 48,
                )
                PixelButton(
                    text = stringResource(R.string.menu_quit),
                    icon = Icons.Filled.PowerSettingsNew,
                    container = cs.error,
                    onContainer = cs.onError,
                    onClick = onQuit,
                    modifier = Modifier.weight(1f),
                    height = 48,
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        MenuBackdrop()
        if (landscape) {
            // Wide and short: controls, title and Bytey on the left, the buttons on the right.
            Row(
                Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    controls()
                    Spacer(Modifier.height(12.dp))
                    GameTitle(Modifier.fillMaxWidth(), compact = true)
                    Spacer(Modifier.height(8.dp))
                    mascot()
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) { buttons() }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
            ) {
                controls()
                Spacer(Modifier.height(20.dp))
                GameTitle(Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { mascot() }
                Spacer(Modifier.height(8.dp))
                buttons()
            }
        }
    }
}
