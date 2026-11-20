package com.bytewise.game.domain

/**
 * Pure rules for moving between the questions of one level. Kept free of Android so they can be unit-tested.
 *
 * [frontier] is the lowest index that has not been answered or expired yet (== number of resolved questions).
 * The player may visit any index <= frontier; the frontier question itself is only "reachable" by moving forward
 * from a resolved question, never by skipping an unresolved one.
 */
object NavigationRules {
    fun canGoPrevious(index: Int): Boolean = index > 0

    /** Forward is allowed only when the current question is resolved (answered / expired) and a next one exists. */
    fun canGoNext(index: Int, total: Int, currentResolved: Boolean): Boolean =
        currentResolved && index + 1 < total

    /** The level can be finished only from the last question, once every question is resolved. */
    fun canFinish(index: Int, total: Int, resolvedCount: Int): Boolean =
        index == total - 1 && resolvedCount == total

    /** An answer may be changed only if the question was answered (not expired) and still has time on its clock. */
    fun canChangeAnswer(answered: Boolean, timedOut: Boolean, remainingMs: Long): Boolean =
        answered && !timedOut && remainingMs > 0L
}
