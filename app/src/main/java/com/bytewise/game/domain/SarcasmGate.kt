package com.bytewise.game.domain

/**
 * Keeps Bytey's optional sarcastic remarks occasional: at most one per [minGapMs], never two in a row without
 * that gap. Time is passed in so the rule is deterministic and unit-testable.
 */
class SarcasmGate(private val minGapMs: Long = 20_000L) {
    private var lastAt = Long.MIN_VALUE / 2

    /** Returns true (and records the moment) when a remark may be made now. */
    fun tryAcquire(nowMs: Long): Boolean {
        if (nowMs - lastAt < minGapMs) return false
        lastAt = nowMs
        return true
    }
}
