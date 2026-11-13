package com.bytewise.game.data.local

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/** Small "where was I" record for the run in progress; the answers themselves live in Room. */
val Context.gameStateDataStore by preferencesDataStore(name = "bytewise_game_state")

object GameStateKeys {
    val SESSION_ID = longPreferencesKey("session_id")
    val LEVEL = intPreferencesKey("level")
    val ORDER = stringPreferencesKey("question_order")
    val INDEX = intPreferencesKey("index")
    val REMAINING_MS = longPreferencesKey("remaining_ms")
}
