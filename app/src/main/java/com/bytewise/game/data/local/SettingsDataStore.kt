package com.bytewise.game.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(name = "bytewise_settings")

object SettingsKeys {
    val LANGUAGE = stringPreferencesKey("language")
    // The old "theme_mode" key is no longer read; a value left over from an earlier version is simply ignored.
    val SOUND = booleanPreferencesKey("sound_enabled")
    val BYTEY_MUTED = booleanPreferencesKey("bytey_muted")
}
