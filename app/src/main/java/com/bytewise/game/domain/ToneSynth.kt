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
        /** Pitch glide target; 0 keeps [frequencyHz] constant. Used for growls, squeaks and sighs. */
        val endFrequencyHz: Double = 0.0,
        /** Vibrato rate in Hz and depth as a fraction of the pitch (0 = none). */
        val vibratoHz: Double = 0.0,
        val vibratoDepth: Double = 0.0,
        /** 0..1 share of noise mixed into the tone (rasp / breath); deterministic, so renders are repeatable. */
        val noise: Float = 0f,
        /** Sawtooth instead of sine/square: a harsher, throatier body for roars. */
        val saw: Boolean = false,
    )

    fun durationMs(notes: List<Note>): Int = notes.sumOf { it.durationMs }

    private fun samplesFor(ms: Int): Int = SAMPLE_RATE * ms / 1000

    fun render(notes: List<Note>): ShortArray {
        val out = ShortArray(notes.sumOf { samplesFor(it.durationMs) })
        var pos = 0
        val attack = (SAMPLE_RATE * 0.005).toInt().coerceAtLeast(1)
        val rng = java.util.Random(7L) // fixed seed: the same cue always renders the same samples
        for (note in notes) {
            val n = samplesFor(note.durationMs)
            val release = (n * 0.25).toInt().coerceAtLeast(1)
            if (note.frequencyHz > 0.0) {
                var phase = 0.0
                val end = if (note.endFrequencyHz > 0.0) note.endFrequencyHz else note.frequencyHz
                for (i in 0 until n) {
                    val t = if (n > 1) i.toDouble() / (n - 1) else 0.0
                    var f = note.frequencyHz + (end - note.frequencyHz) * t
                    if (note.vibratoDepth > 0.0) {
                        f *= 1.0 + note.vibratoDepth * sin(2.0 * PI * note.vibratoHz * i / SAMPLE_RATE)
                    }
                    phase += 2.0 * PI * f / SAMPLE_RATE
                    val tone = when {
                        note.saw -> {
                            val cycle = (phase / (2.0 * PI)) % 1.0
                            2.0 * cycle - 1.0
                        }
                        note.square -> if (sin(phase) >= 0.0) 1.0 else -1.0
                        else -> sin(phase)
                    }
                    val wave = if (note.noise > 0f) {
                        tone * (1.0 - note.noise) + (rng.nextDouble() * 2.0 - 1.0) * note.noise
                    } else {
                        tone
                    }
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

/** A voiced, gliding sound: the building block of Bytey's dinosaur voice. */
private fun voice(
    fromHz: Double,
    toHz: Double,
    ms: Int,
    volume: Float = 0.5f,
    vibratoHz: Double = 0.0,
    vibratoDepth: Double = 0.0,
    noise: Float = 0f,
    saw: Boolean = true,
) = ToneSynth.Note(fromHz, ms, volume, false, toHz, vibratoHz, vibratoDepth, noise, saw)

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
    /** Happy dino chirp: two quick rising squeaks with a little rasp. */
    BYTEY_HAPPY(true, listOf(voice(520.0, 900.0, 90, 0.5f, 22.0, 0.04, 0.12f), rest(25), voice(640.0, 1150.0, 130, 0.5f, 24.0, 0.05, 0.1f))),
    /** Sad dino whine: a long falling wobble. */
    BYTEY_SAD(true, listOf(voice(520.0, 190.0, 420, 0.5f, 9.0, 0.06, 0.15f))),
    /** Tap: a short, cute squeak. */
    BYTEY_TAP(true, listOf(voice(380.0, 980.0, 110, 0.5f, 28.0, 0.05, 0.08f), voice(980.0, 700.0, 70, 0.45f, 0.0, 0.0, 0.08f))),
    /** Tap (rare): a small but proud roar. */
    BYTEY_ROAR(true, listOf(voice(210.0, 120.0, 520, 0.55f, 31.0, 0.09, 0.42f), voice(120.0, 70.0, 260, 0.5f, 27.0, 0.08, 0.5f))),
    /** Sarcastic "heh heh heh": three short, descending clucks. */
    BYTEY_GIGGLE(true, listOf(voice(420.0, 300.0, 75, 0.5f, 0.0, 0.0, 0.2f), rest(55), voice(400.0, 280.0, 75, 0.5f, 0.0, 0.0, 0.2f), rest(55), voice(380.0, 240.0, 120, 0.5f, 0.0, 0.0, 0.2f))),
    /** Sarcastic deadpan "hmph": a low, short grunt. */
    BYTEY_HMPH(true, listOf(voice(170.0, 105.0, 260, 0.55f, 14.0, 0.05, 0.55f))),
}

object SoundPolicy {
    /** Game sound is the master switch; Bytey's chirps additionally need Bytey to be un-muted. */
    fun isAudible(cue: SoundCue, soundEnabled: Boolean, byteyMuted: Boolean): Boolean =
        soundEnabled && !(cue.byteyVoice && byteyMuted)
}
