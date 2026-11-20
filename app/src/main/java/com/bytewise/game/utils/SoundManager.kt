package com.bytewise.game.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.domain.SoundCue
import com.bytewise.game.domain.SoundPolicy
import com.bytewise.game.domain.ToneSynth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Anything that can play a cue; lets view-model-free classes (the Bytey controller) stay testable. */
fun interface CuePlayer {
    fun play(cue: SoundCue)
}

/** Reads Bytey's remarks aloud. */
fun interface TextSpeaker {
    fun speak(text: String, locale: Locale)
}

/**
 * The single audio engine of the app (one instance in AppContainer).
 *
 *  - Two channels: effects (correct / wrong / tick ...) and Bytey's voice. A new sound on a channel stops the
 *    previous one on that channel, so sounds never pile up; an effect and a voice may sound together.
 *  - The sound switch and the Bytey mute are observed from the saved settings (DataStore, so they survive
 *    restarts). Turning sound off stops everything that is playing right away.
 *  - All AudioTrack work happens on one serial worker; every track is stopped and released when it ends, when it
 *    is replaced, on [stopAll] (screen left / app stopped) and on [release]. Audio is optional: failures are
 *    swallowed so they can never crash gameplay.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SoundManager(context: Context, settings: Flow<AppSettings>) : CuePlayer, TextSpeaker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val cache = HashMap<SoundCue, ShortArray>()

    @Volatile private var soundEnabled = true
    @Volatile private var byteyMuted = false

    /**
     * Android's built-in text-to-speech reads Bytey's remarks. It needs no extra dependency or permission; whether a
     * language (Arabic in particular) is spoken depends on the voices installed on the device. If the language is
     * not available the remark is simply shown, never an error.
     */
    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false
    private val appContext = context.applicationContext

    private val effects = Channel()
    private val voice = Channel()

    init {
        settings.onEach { s ->
            soundEnabled = s.soundEnabled
            byteyMuted = s.byteyMuted
            if (!s.soundEnabled) stopAll() else if (s.byteyMuted) voice.stop()
        }.launchIn(scope)
    }

    override fun play(cue: SoundCue) {
        if (!SoundPolicy.isAudible(cue, soundEnabled, byteyMuted)) return
        val channel = if (cue.byteyVoice) voice else effects
        scope.launch {
            // Re-check on the worker: the switch may have been turned off while this call was queued.
            if (!SoundPolicy.isAudible(cue, soundEnabled, byteyMuted)) return@launch
            // A tick must never cut off a real effect that is still ringing.
            if (cue == SoundCue.TICK && channel.busy && channel.current != SoundCue.TICK) return@launch
            channel.start(cue)
        }
    }

    /** Stops everything that is sounding (sound switched off, screen left, app in the background). */
    fun stopAll() {
        scope.launch {
            effects.stop()
            voice.stop()
            stopSpeech()
        }
    }

    /** Frees the worker; call when the process is going away. */
    fun release() {
        effects.stop()
        voice.stop()
        stopSpeech()
        try { tts?.shutdown() } catch (_: Exception) { }
        tts = null
        scope.cancel()
    }

    override fun speak(text: String, locale: Locale) {
        if (!soundEnabled || byteyMuted || text.isBlank()) return
        scope.launch {
            if (!soundEnabled || byteyMuted) return@launch
            try {
                var engine = tts
                if (engine == null) {
                    // First use: create the engine; the remark that triggered it is spoken once it is ready.
                    pendingSpeech = text to locale
                    tts = TextToSpeech(appContext) { status ->
                        ttsReady = status == TextToSpeech.SUCCESS
                        if (ttsReady) pendingSpeech?.let { (t, l) -> scope.launch { say(t, l) } }
                        pendingSpeech = null
                    }
                    return@launch
                }
                if (ttsReady) say(text, locale)
            } catch (_: Exception) { }
        }
    }

    @Volatile private var pendingSpeech: Pair<String, Locale>? = null

    private fun say(text: String, locale: Locale) {
        val engine = tts ?: return
        if (!soundEnabled || byteyMuted) return
        try {
            val result = engine.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return
            engine.setPitch(1.45f) // a little squeaky: it is a tiny dinosaur
            engine.setSpeechRate(1.05f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "bytey")
        } catch (_: Exception) { }
    }

    private fun stopSpeech() {
        pendingSpeech = null
        try { tts?.stop() } catch (_: Exception) { }
    }

    private fun pcm(cue: SoundCue): ShortArray = cache.getOrPut(cue) { ToneSynth.render(cue.notes) }

    private inner class Channel {
        private var track: AudioTrack? = null
        private var job: Job? = null
        var current: SoundCue? = null
            private set
        val busy: Boolean get() = job?.isActive == true

        /** Must run on the worker. */
        fun start(cue: SoundCue) {
            stop()
            val data = try { pcm(cue) } catch (_: Exception) { return }
            if (data.isEmpty()) return
            val t = try {
                AudioTrack.Builder()
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
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } catch (_: Exception) {
                return
            }
            if (t.state != AudioTrack.STATE_INITIALIZED) {
                t.release()
                return
            }
            try {
                t.write(data, 0, data.size)
                t.play()
            } catch (_: Exception) {
                t.release()
                return
            }
            track = t
            current = cue
            val ms = ToneSynth.durationMs(cue.notes) + 60L
            job = scope.launch {
                delay(ms)
                if (track === t) stop()
            }
        }

        /** Must run on the worker. */
        fun stop() {
            job?.cancel()
            job = null
            current = null
            val t = track ?: return
            track = null
            try { t.stop() } catch (_: Exception) { }
            try { t.release() } catch (_: Exception) { }
        }
    }
}
