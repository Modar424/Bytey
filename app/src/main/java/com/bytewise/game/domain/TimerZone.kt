package com.bytewise.game.domain

/** Timer colour zones: green above 50 %, yellow from 25 % to 50 %, red below 25 % of the time left. */
enum class TimerZone {
    GREEN, YELLOW, RED;

    companion object {
        fun of(remainingFraction: Float): TimerZone = when {
            remainingFraction > 0.5f -> GREEN
            remainingFraction >= 0.25f -> YELLOW
            else -> RED
        }
    }
}

/** When the countdown "tick" sound plays: once for each of the last [LAST_SECONDS] whole seconds. */
object TickPolicy {
    const val LAST_SECONDS = 5
    fun shouldTick(secondsLeft: Int): Boolean = secondsLeft in 1..LAST_SECONDS
}
