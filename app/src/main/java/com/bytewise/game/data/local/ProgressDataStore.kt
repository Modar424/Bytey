package com.bytewise.game.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.progressDataStore by preferencesDataStore(name = "bytewise_progress")

object ProgressKeys {
    val UNLOCKED = stringSetPreferencesKey("unlocked_levels")
    val COMPLETED = stringSetPreferencesKey("completed_levels")
    const val BEST_SCORE_PREFIX = "best_score_"
}
