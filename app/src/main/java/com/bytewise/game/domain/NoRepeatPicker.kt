package com.bytewise.game.domain

import kotlin.random.Random

/** Random index picker that never returns the same index twice in a row for the same key. */
class NoRepeatPicker(private val random: Random = Random.Default) {
    private val last = HashMap<Any, Int>()

    fun next(key: Any, size: Int): Int {
        require(size > 0) { "size must be positive" }
        if (size == 1) return 0
        var i: Int
        do {
            i = random.nextInt(size)
        } while (i == last[key])
        last[key] = i
        return i
    }
}
