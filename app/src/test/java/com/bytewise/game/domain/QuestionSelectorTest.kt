package com.bytewise.game.domain

import com.bytewise.game.data.model.Question
import com.bytewise.game.data.model.QuestionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class QuestionSelectorTest {
    private fun question(id: String, category: QuestionCategory) = Question(
        id = id, level = 1, category = category, conceptId = "c",
        textAr = "س", textEn = "q",
        optionsAr = listOf("1", "2", "3", "4"), optionsEn = listOf("1", "2", "3", "4"),
        correctIndex = 0, explanationAr = "ش", explanationEn = "e", difficulty = 1,
    )

    private fun level(perCategory: Int = 5): List<Question> =
        QuestionCategory.entries.flatMap { c -> (1..perCategory).map { question("${c.name}_$it", c) } }

    @Test fun orderKeepsFiveQuestionsPerCategory() {
        val order = QuestionSelector.buildOrder(level(), Random(1))
        assertEquals(25, order.size)
        assertEquals(25, order.map { it.id }.toSet().size)
        QuestionCategory.entries.forEach { c -> assertEquals(5, order.count { it.category == c }) }
    }

    @Test fun orderIsActuallyShuffledAndDiffersBetweenRuns() {
        val source = level()
        val a = QuestionSelector.buildOrder(source, Random(1)).map { it.id }
        val b = QuestionSelector.buildOrder(source, Random(2)).map { it.id }
        assertNotEquals(source.map { it.id }, a)
        assertNotEquals(a, b)
    }

    @Test fun extraQuestionsInACategoryAreNotAllUsed() {
        val order = QuestionSelector.buildOrder(level(perCategory = 8), Random(3))
        assertEquals(25, order.size)
        QuestionCategory.entries.forEach { c -> assertEquals(5, order.count { it.category == c }) }
    }

    @Test fun restoreOrderKeepsTheSavedSequence() {
        val source = level()
        val ids = listOf("TOKENS_2", "GENERAL_1", "MODELS_5")
        assertEquals(ids, QuestionSelector.restoreOrder(ids, source)?.map { it.id })
    }

    @Test fun restoreOrderFailsWhenAQuestionNoLongerExists() {
        assertNull(QuestionSelector.restoreOrder(listOf("GENERAL_1", "GONE"), level()))
        assertNotNull(QuestionSelector.restoreOrder(emptyList(), level()))
    }
}
