package com.bytewise.game.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.domain.SoundCue
import com.bytewise.game.domain.SoundPolicy
import com.bytewise.game.domain.ToneSynth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays the synthesised cues with AudioTrack (no audio files, works offline). Honours both toggles
 * through [SoundPolicy]. Audio is optional: any failure is swallowed so it can never crash gameplay.
 */
class SoundEffects {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = HashMap<SoundCue, ShortArray>()

    fun play(cue: SoundCue, settings: AppSettings) {
        if (!SoundPolicy.isAudible(cue, settings.soundEnabled, settings.byteyMuted)) return
        scope.launch {
            try {
                playBlocking(cue)
            } catch (_: Exception) {
                // Sound is a nice-to-have; ignore device/audio-focus problems.
            }
        }
    }

    private suspend fun playBlocking(cue: SoundCue) {
        val pcm = synchronized(cache) { cache.getOrPut(cue) { ToneSynth.render(cue.notes) } }
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(ToneSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            delay(ToneSynth.durationMs(cue.notes) + 80L)
        } finally {
            track.release()
        }
    }

    fun release() {
        scope.cancel()
    }
}
