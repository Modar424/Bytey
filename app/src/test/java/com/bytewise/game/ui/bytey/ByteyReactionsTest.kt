package com.bytewise.game.ui.bytey

import com.bytewise.game.domain.SoundCue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ByteyReactionsTest {
    @Test fun answerReactionsFollowThePriorityOrder() {
        assertEquals(ByteyEvent.TIMEOUT, ByteyReactions.forAnswer(false, timedOut = true, isFast = false, streakMilestone = false))
        assertEquals(ByteyEvent.INCORRECT, ByteyReactions.forAnswer(false, false, false, false))
        assertEquals(ByteyEvent.STREAK, ByteyReactions.forAnswer(true, false, isFast = true, streakMilestone = true))
        assertEquals(ByteyEvent.FAST_CORRECT, ByteyReactions.forAnswer(true, false, isFast = true, streakMilestone = false))
        assertEquals(ByteyEvent.CORRECT, ByteyReactions.forAnswer(true, false, false, false))
    }

    @Test fun levelResultReactions() {
        assertEquals(ByteyEvent.LEVEL_COMPLETE_LOW, ByteyReactions.forLevelResult(10, passed = false, newBest = true, unlockedNext = false))
        assertEquals(ByteyEvent.LEVEL_UNLOCKED, ByteyReactions.forLevelResult(22, passed = true, newBest = true, unlockedNext = true))
        assertEquals(ByteyEvent.PERSONAL_BEST, ByteyReactions.forLevelResult(16, passed = true, newBest = true, unlockedNext = false))
        assertEquals(ByteyEvent.LEVEL_COMPLETE_HIGH, ByteyReactions.forLevelResult(20, passed = true, newBest = false, unlockedNext = false))
        assertEquals(ByteyEvent.LEVEL_COMPLETE_AVERAGE, ByteyReactions.forLevelResult(17, passed = true, newBest = false, unlockedNext = false))
    }

    @Test fun answerEventsPlayASoundAndAByteyChirp() {
        listOf(ByteyEvent.CORRECT, ByteyEvent.FAST_CORRECT, ByteyEvent.STREAK, ByteyEvent.INCORRECT, ByteyEvent.TIMEOUT).forEach { e ->
            val cues = ByteyReactions.cuesFor(e)
            assertTrue("$e has no game sound", cues.any { !it.byteyVoice })
            assertTrue("$e has no Bytey chirp", cues.any { it.byteyVoice })
        }
        assertEquals(listOf(SoundCue.WRONG, SoundCue.BYTEY_SAD), ByteyReactions.cuesFor(ByteyEvent.INCORRECT))
    }

    @Test fun quietEventsHaveNoSound() {
        assertTrue(ByteyReactions.cuesFor(ByteyEvent.MENU_GREETING).isEmpty())
        assertTrue(ByteyReactions.cuesFor(ByteyEvent.LEVELS_TIP).isEmpty())
    }

    @Test fun reportReactionCelebratesImprovement() {
        assertEquals(ByteyEvent.IMPROVEMENT, ByteyReactions.forReport(hasImprovement = true))
        assertEquals(ByteyEvent.LEARNING_REPORT, ByteyReactions.forReport(hasImprovement = false))
    }
}
