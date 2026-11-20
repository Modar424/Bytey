package com.bytewise.game.ui.bytey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Occasional unprompted sarcastic remark: waits a random 25-45 s, then lets Bytey speak (bubble, animation and
 * voice together). The message never repeats back-to-back (NoRepeatPicker inside the controller). Cancelled
 * automatically when the screen is left, so nothing is said or played afterwards.
 */
@Composable
fun IdleSarcasm(controller: ByteyController, minSeconds: Int = 25, maxSeconds: Int = 45) {
    val resources = LocalContext.current.resources
    LaunchedEffect(controller) {
        while (true) {
            delay(Random.nextLong(minSeconds * 1000L, maxSeconds * 1000L))
            controller.react(ByteyEvent.SARCASM, resources)
        }
    }
}
