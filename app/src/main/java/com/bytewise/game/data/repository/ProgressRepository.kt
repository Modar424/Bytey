package com.bytewise.game.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.bytewise.game.data.local.ProgressKeys
import com.bytewise.game.data.local.progressDataStore
import com.bytewise.game.data.model.ProgressState
import com.bytewise.game.domain.LevelRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Unlocked levels, completed levels and best scores (small values -> DataStore). */
class ProgressRepository(context: Context) {
    private val store = context.applicationContext.progressDataStore

    val progress: Flow<ProgressState> = store.data.map { p ->
        ProgressState(
            unlockedLevels = p[ProgressKeys.UNLOCKED].toIntSet() + 1, // level 1 is always open
            completedLevels = p[ProgressKeys.COMPLETED].toIntSet(),
            bestScores = readBestScores(p),
        )
    }

    suspend fun recordLevelResult(level: Int, score: Int, passed: Boolean, totalLevels: Int) {
        store.edit { p ->
            val bestKey = intPreferencesKey(ProgressKeys.BEST_SCORE_PREFIX + level)
            if (score > (p[bestKey] ?: 0)) p[bestKey] = score
            if (passed) {
                p[ProgressKeys.COMPLETED] = p[ProgressKeys.COMPLETED].orEmpty() + level.toString()
                LevelRules.nextLevelToUnlock(level, totalLevels)?.let { next ->
                    p[ProgressKeys.UNLOCKED] = p[ProgressKeys.UNLOCKED].orEmpty() + next.toString()
                }
            }
        }
    }

    private fun readBestScores(p: Preferences): Map<Int, Int> =
        p.asMap().entries.mapNotNull { (key, value) ->
            val level = key.name.removePrefix(ProgressKeys.BEST_SCORE_PREFIX).toIntOrNull()
            if (key.name.startsWith(ProgressKeys.BEST_SCORE_PREFIX) && level != null && value is Int) level to value else null
        }.toMap()

    private fun Set<String>?.toIntSet(): Set<Int> = orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
}
