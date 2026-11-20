package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneSynthTest {
    @Test fun lengthMatchesTheSumOfTheNotes() {
        val notes = listOf(ToneSynth.Note(440.0, 100), ToneSynth.Note(0.0, 50), ToneSynth.Note(660.0, 200))
        assertEquals(ToneSynth.SAMPLE_RATE * 350 / 1000, ToneSynth.render(notes).size)
        assertEquals(350, ToneSynth.durationMs(notes))
    }

    @Test fun restsAreSilentAndNotesAreAudibleWithoutClipping() {
        val rest = ToneSynth.render(listOf(ToneSynth.Note(0.0, 100)))
        assertTrue(rest.all { it.toInt() == 0 })
        val tone = ToneSynth.render(listOf(ToneSynth.Note(440.0, 100, volume = 0.5f)))
        val peak = tone.maxOf { abs(it.toInt()) }
        assertTrue(peak > 1000)
        assertTrue(peak <= Short.MAX_VALUE / 2 + 1)
    }

    @Test fun everyCueRendersSomething() {
        SoundCue.entries.forEach { cue ->
            val pcm = ToneSynth.render(cue.notes)
            assertTrue("${cue.name} is empty", pcm.isNotEmpty())
            assertTrue("${cue.name} is silent", pcm.any { it.toInt() != 0 })
        }
    }

    @Test fun gameSoundToggleIsTheMasterSwitch() {
        SoundCue.entries.forEach { cue ->
            assertFalse(SoundPolicy.isAudible(cue, soundEnabled = false, byteyMuted = false))
            assertFalse(SoundPolicy.isAudible(cue, soundEnabled = false, byteyMuted = true))
        }
    }

    @Test fun byteyMuteSilencesOnlyByteysChirps() {
        assertFalse(SoundPolicy.isAudible(SoundCue.BYTEY_HAPPY, soundEnabled = true, byteyMuted = true))
        assertFalse(SoundPolicy.isAudible(SoundCue.BYTEY_SAD, soundEnabled = true, byteyMuted = true))
        assertTrue(SoundPolicy.isAudible(SoundCue.CORRECT, soundEnabled = true, byteyMuted = true))
        assertTrue(SoundPolicy.isAudible(SoundCue.BYTEY_HAPPY, soundEnabled = true, byteyMuted = false))
    }
}
