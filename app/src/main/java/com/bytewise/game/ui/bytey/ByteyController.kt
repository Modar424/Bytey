package com.bytewise.game.ui.bytey

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.domain.NoRepeatPicker
import com.bytewise.game.utils.CuePlayer
import com.bytewise.game.utils.TextSpeaker
import kotlinx.coroutines.delay

/**
 * Holds what Bytey is currently doing. Call [react] from a LaunchedEffect: a newer reaction
 * cancels the previous one (the old coroutine is cancelled), then Bytey returns to idle.
 */
@Stable
class ByteyController(
    private val picker: NoRepeatPicker = NoRepeatPicker(),
    /** Plays the reaction's sounds at the same moment the animation and bubble appear. */
    private val sounds: CuePlayer? = null,
    /** Reads the shown remark aloud in the app's current language. */
    private val speaker: TextSpeaker? = null,
) {
    /** Bumped on every tap so the view can play a squash-and-bounce animation. */
    var tapCount by mutableIntStateOf(0)
        private set

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
        ByteyReactions.cuesFor(event).forEach { sounds?.play(it) }
        message?.let { if (ByteyReactions.isSpoken(event)) speaker?.speak(it, resources.configuration.locales[0]) }
        delay(event.holdMs)
        message = null
        state = ByteyState.IDLE
        mode = ByteyDisplayMode.NORMAL
    }

    /** Tap reaction: bounce, a rotating voice (squeak / roar / giggle) and a playful remark, all at once. */
    suspend fun tap(resources: Resources) {
        tapCount++
        val voices = ByteyReactions.tapVoices
        sounds?.play(voices[picker.next(TAP_VOICE_KEY, voices.size)])
        val options = resources.getStringArray(ByteyEvent.TAP.messages)
        state = ByteyEvent.TAP.state
        mode = ByteyEvent.TAP.mode
        message = if (options.isEmpty()) null else options[picker.next(ByteyEvent.TAP, options.size)]
        message?.let { speaker?.speak(it, resources.configuration.locales[0]) }
        delay(ByteyEvent.TAP.holdMs)
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

private const val TAP_VOICE_KEY = "tap_voice"

/** The controller is wired to the app-wide [com.bytewise.game.utils.SoundManager] so every screen's Bytey has a voice. */
@Composable
fun rememberByteyController(): ByteyController {
    val context = LocalContext.current
    return remember {
        val app = context.applicationContext as? BytewiseApplication
        ByteyController(sounds = app?.container?.soundManager, speaker = app?.container?.soundManager)
    }
}
