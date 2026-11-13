package com.bytewise.game.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.bytewise.game.data.local.SettingsKeys
import com.bytewise.game.data.local.settingsDataStore
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            language = AppLanguage.fromTag(p[SettingsKeys.LANGUAGE]),
            themeMode = p[SettingsKeys.THEME]
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            soundEnabled = p[SettingsKeys.SOUND] ?: true,
            byteyMuted = p[SettingsKeys.BYTEY_MUTED] ?: false,
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        store.edit { it[SettingsKeys.LANGUAGE] = language.tag }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[SettingsKeys.THEME] = mode.name }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        store.edit { it[SettingsKeys.SOUND] = enabled }
    }

    suspend fun setByteyMuted(muted: Boolean) {
        store.edit { it[SettingsKeys.BYTEY_MUTED] = muted }
    }
}
