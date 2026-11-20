package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerZoneTest {
    @Test fun greenAboveHalf() {
        assertEquals(TimerZone.GREEN, TimerZone.of(1f))
        assertEquals(TimerZone.GREEN, TimerZone.of(0.51f))
    }

    @Test fun yellowFromQuarterToHalf() {
        assertEquals(TimerZone.YELLOW, TimerZone.of(0.5f))
        assertEquals(TimerZone.YELLOW, TimerZone.of(0.3f))
        assertEquals(TimerZone.YELLOW, TimerZone.of(0.25f))
    }

    @Test fun redBelowQuarter() {
        assertEquals(TimerZone.RED, TimerZone.of(0.249f))
        assertEquals(TimerZone.RED, TimerZone.of(0f))
    }

    @Test fun twentySecondTimerMatchesTheZonesInSeconds() {
        fun zone(secondsLeft: Int) = TimerZone.of(secondsLeft * 1000f / GameConfig.QUESTION_TIME_MS)
        assertEquals(TimerZone.GREEN, zone(11))
        assertEquals(TimerZone.YELLOW, zone(10))
        assertEquals(TimerZone.YELLOW, zone(5))
        assertEquals(TimerZone.RED, zone(4))
    }

    @Test fun tickPlaysOnlyDuringTheLastFiveWholeSeconds() {
        assertEquals(listOf(5, 4, 3, 2, 1), (0..20).filter { TickPolicy.shouldTick(it) }.sortedDescending())
    }

    @Test fun tickIsAGameSoundThatFollowsTheMasterSwitch() {
        assertEquals(false, SoundCue.TICK.byteyVoice)
        assertEquals(false, SoundPolicy.isAudible(SoundCue.TICK, soundEnabled = false, byteyMuted = false))
        assertEquals(true, SoundPolicy.isAudible(SoundCue.TICK, soundEnabled = true, byteyMuted = true))
    }
}
