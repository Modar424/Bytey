package com.bytewise.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NoRepeatPickerTest {
    @Test fun neverRepeatsConsecutivelyForSameKey() {
        val picker = NoRepeatPicker()
        var prev = -1
        repeat(500) {
            val i = picker.next("k", 3)
            assertNotEquals(prev, i)
            prev = i
        }
    }

    @Test fun singleOptionAlwaysZero() {
        val picker = NoRepeatPicker()
        repeat(5) { assertEquals(0, picker.next("k", 1)) }
    }
}
