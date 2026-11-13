package com.bytewise.game.ui.bytey

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bytewise.game.domain.NoRepeatPicker
import kotlinx.coroutines.delay

/**
 * Holds what Bytey is currently doing. Call [react] from a LaunchedEffect: a newer reaction
 * cancels the previous one (the old coroutine is cancelled), then Bytey returns to idle.
 */
@Stable
class ByteyController(private val picker: NoRepeatPicker = NoRepeatPicker()) {
    var state by mutableStateOf(ByteyState.IDLE)
        private set
    var mode by mutableStateOf(ByteyDisplayMode.NORMAL)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    /** [resources] must come from the localized context so the message matches the selected language. */
    suspend fun react(event: ByteyEvent, resources: Resources) {
        val options = resources.getStringArray(event.messages)
        state = event.state
        mode = event.mode
        message = if (options.isEmpty()) null else options[picker.next(event, options.size)]
        delay(event.holdMs)
        message = null
        state = ByteyState.IDLE
        mode = ByteyDisplayMode.NORMAL
    }

    fun clear() {
        message = null
        state = ByteyState.IDLE
        mode = ByteyDisplayMode.NORMAL
    }
}

@Composable
fun rememberByteyController(): ByteyController = remember { ByteyController() }
