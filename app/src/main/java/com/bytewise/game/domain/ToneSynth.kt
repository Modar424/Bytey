package com.bytewise.game.domain

import kotlin.math.PI
import kotlin.math.sin

/** Tiny offline sound generator: no audio assets are needed, everything is synthesised from notes. */
object ToneSynth {
    const val SAMPLE_RATE = 22_050

    /** [frequencyHz] 0 is a rest. */
    data class Note(
        val frequencyHz: Double,
        val durationMs: Int,
        val volume: Float = 0.45f,
        val square: Boolean = false,
    )

    fun durationMs(notes: List<Note>): Int = notes.sumOf { it.durationMs }

    private fun samplesFor(ms: Int): Int = SAMPLE_RATE * ms / 1000

    fun render(notes: List<Note>): ShortArray {
        val out = ShortArray(notes.sumOf { samplesFor(it.durationMs) })
        var pos = 0
        val attack = (SAMPLE_RATE * 0.005).toInt().coerceAtLeast(1)
        for (note in notes) {
            val n = samplesFor(note.durationMs)
            val release = (n * 0.25).toInt().coerceAtLeast(1)
            if (note.frequencyHz > 0.0) {
                for (i in 0 until n) {
                    val phase = 2.0 * PI * note.frequencyHz * i / SAMPLE_RATE
                    val wave = if (note.square) (if (sin(phase) >= 0.0) 1.0 else -1.0) else sin(phase)
                    val envelope = minOf(1.0, i.toDouble() / attack, (n - i).toDouble() / release)
                    out[pos + i] = (wave * envelope * note.volume * Short.MAX_VALUE).toInt().toShort()
                }
            }
            pos += n
        }
        return out
    }
}

private fun tone(frequencyHz: Double, ms: Int, volume: Float = 0.45f, square: Boolean = false) =
    ToneSynth.Note(frequencyHz, ms, volume, square)

private fun rest(ms: Int) = ToneSynth.Note(0.0, ms)

/** Every sound the game can play. [byteyVoice] cues are Bytey's own chirps and obey the Bytey mute. */
enum class SoundCue(val byteyVoice: Boolean, val notes: List<ToneSynth.Note>) {
    CORRECT(false, listOf(tone(659.25, 90), tone(783.99, 90), tone(1046.5, 160))),
    WRONG(false, listOf(tone(220.0, 140, square = true), tone(165.0, 240, square = true))),
    TIMEOUT(false, listOf(tone(330.0, 120), rest(40), tone(262.0, 120), rest(40), tone(196.0, 260))),
    STREAK(false, listOf(tone(523.25, 80), tone(659.25, 80), tone(783.99, 80), tone(1046.5, 80), tone(783.99, 80), tone(1046.5, 220))),
    LEVEL_PASS(false, listOf(tone(523.25, 130), tone(659.25, 130), tone(783.99, 130), tone(1046.5, 360))),
    LEVEL_FAIL(false, listOf(tone(392.0, 170), tone(329.63, 170), tone(261.63, 380))),
    /** Short click for each of the last five seconds of a question. */
    TICK(false, listOf(tone(1200.0, 30, 0.22f, true))),
    BYTEY_HAPPY(true, listOf(tone(880.0, 50, 0.3f, true), tone(1174.66, 50, 0.3f, true), tone(1479.98, 90, 0.3f, true))),
    BYTEY_SAD(true, listOf(tone(600.0, 80, 0.3f, true), tone(450.0, 80, 0.3f, true), tone(330.0, 150, 0.3f, true))),
}

object SoundPolicy {
    /** Game sound is the master switch; Bytey's chirps additionally need Bytey to be un-muted. */
    fun isAudible(cue: SoundCue, soundEnabled: Boolean, byteyMuted: Boolean): Boolean =
        soundEnabled && !(cue.byteyVoice && byteyMuted)
}
