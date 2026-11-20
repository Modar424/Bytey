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
    /** Seconds per question chosen for this level. */
    val timerSeconds: Int = GameConfig.QUESTION_TIME_SECONDS,
    /** Time left per visited question index (so revisiting never resets or extends a question's clock). */
    val remainingByIndex: Map<Int, Long> = emptyMap(),
)

internal fun encodeRemaining(map: Map<Int, Long>): String =
    map.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }

internal fun decodeRemaining(text: String?): Map<Int, Long> {
    if (text.isNullOrBlank()) return emptyMap()
    val out = HashMap<Int, Long>()
    for (part in text.split(',')) {
        val bits = part.split(':')
        val k = bits.getOrNull(0)?.toIntOrNull() ?: continue
        val v = bits.getOrNull(1)?.toLongOrNull() ?: continue
        out[k] = v
    }
    return out
}

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
            timerSeconds = GameConfig.normalizeTimer(p[GameStateKeys.TIMER_SECONDS] ?: GameConfig.QUESTION_TIME_SECONDS),
            remainingByIndex = decodeRemaining(p[GameStateKeys.REMAINING_MAP]),
        )
    }

    suspend fun save(game: SavedGame) {
        store.edit { p ->
            p[GameStateKeys.SESSION_ID] = game.sessionId
            p[GameStateKeys.LEVEL] = game.level
            p[GameStateKeys.ORDER] = game.questionIds.joinToString(",")
            p[GameStateKeys.INDEX] = game.index
            p[GameStateKeys.REMAINING_MS] = game.remainingMs
            p[GameStateKeys.TIMER_SECONDS] = game.timerSeconds
            p[GameStateKeys.REMAINING_MAP] = encodeRemaining(game.remainingByIndex)
        }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }
}
