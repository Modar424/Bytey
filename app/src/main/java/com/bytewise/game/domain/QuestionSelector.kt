package com.bytewise.game.domain

import com.bytewise.game.data.model.Question
import kotlin.random.Random

object QuestionSelector {
    /**
     * Random order for one run of a level that keeps the category distribution: at most
     * [GameConfig.QUESTIONS_PER_CATEGORY] questions per category are picked (all 25 of a standard level),
     * then the whole selection is shuffled.
     */
    fun buildOrder(levelQuestions: List<Question>, random: Random = Random.Default): List<Question> {
        val picked = levelQuestions
            .groupBy { it.category }
            .flatMap { (_, group) -> group.shuffled(random).take(GameConfig.QUESTIONS_PER_CATEGORY) }
        return picked.shuffled(random)
    }

    /** Rebuilds a saved order from question ids; null when any id no longer exists in the bank. */
    fun restoreOrder(ids: List<String>, allQuestions: List<Question>): List<Question>? {
        val byId = allQuestions.associateBy { it.id }
        return ids.map { byId[it] ?: return null }
    }
}
