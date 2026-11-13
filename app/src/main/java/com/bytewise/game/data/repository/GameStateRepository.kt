package com.bytewise.game.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.bytewise.game.data.local.GameStateKeys
import com.bytewise.game.data.local.gameStateDataStore
import com.bytewise.game.domain.GameConfig
import kotlinx.coroutines.flow.first

/**
 * State needed to resume a run after the app was closed or killed: question order, which question
 * is current and how much time was left. Answers/score are rebuilt from Room, never stored here.
 */
data class SavedGame(
    val sessionId: Long,
    val level: Int,
    val questionIds: List<String>,
    val index: Int,
    val remainingMs: Long,
)

class GameStateRepository(context: Context) {
    private val store = context.applicationContext.gameStateDataStore

    suspend fun load(): SavedGame? {
        val p = store.data.first()
        val sessionId = p[GameStateKeys.SESSION_ID] ?: return null
        val level = p[GameStateKeys.LEVEL] ?: return null
        val ids = (p[GameStateKeys.ORDER] ?: return null).split(',').filter { it.isNotBlank() }
        if (ids.isEmpty()) return null
        return SavedGame(
            sessionId = sessionId,
            level = level,
            questionIds = ids,
            index = p[GameStateKeys.INDEX] ?: 0,
            remainingMs = p[GameStateKeys.REMAINING_MS] ?: GameConfig.QUESTION_TIME_MS,
        )
    }

    suspend fun save(game: SavedGame) {
        store.edit { p ->
            p[GameStateKeys.SESSION_ID] = game.sessionId
            p[GameStateKeys.LEVEL] = game.level
            p[GameStateKeys.ORDER] = game.questionIds.joinToString(",")
            p[GameStateKeys.INDEX] = game.index
            p[GameStateKeys.REMAINING_MS] = game.remainingMs
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
