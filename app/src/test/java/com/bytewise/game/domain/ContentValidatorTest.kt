package com.bytewise.game.domain

import com.bytewise.game.data.model.QuestionCategory
import com.bytewise.game.data.model.QuestionDto
import com.bytewise.game.data.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {
    private fun dto(id: String, correct: Int = 1, options: Int = 4, text: String = "Q $id") = QuestionDto(
        id = id, level = 1, category = QuestionCategory.GENERAL, conceptId = "c1", text = text,
        options = List(options) { "opt$it" }, correctIndex = correct, explanation = "why", difficulty = 1,
    )

    private fun merged(en: QuestionDto, ar: QuestionDto): List<Question> =
        ContentMerger.mergeQuestions(listOf(en), listOf(ar)).items

    @Test fun validQuestionHasNoProblems() {
        val q = merged(dto("a"), dto("a"))
        assertTrue(ContentValidator.validateQuestions(q, setOf("c1")).isEmpty())
    }

    @Test fun detectsWrongOptionCountAndBadIndex() {
        val q = merged(dto("a", options = 3, correct = 5), dto("a", options = 3, correct = 5))
        val problems = ContentValidator.validateQuestions(q, setOf("c1"))
        assertTrue(problems.any { "exactly 4 options" in it })
        assertTrue(problems.any { "correctIndex out of range" in it })
    }

    @Test fun mergerReportsDifferentCorrectIndexAndMissingTranslation() {
        val result = ContentMerger.mergeQuestions(listOf(dto("a", correct = 1), dto("b")), listOf(dto("a", correct = 2)))
        assertTrue(result.problems.any { "correctIndex differs" in it })
        assertTrue(result.problems.any { "b exists in English but not in Arabic" in it })
        assertEquals(1, result.items.size)
    }

    @Test fun unknownConceptIsReported() {
        val q = merged(dto("a"), dto("a"))
        assertTrue(ContentValidator.validateQuestions(q, setOf("other")).any { "unknown conceptId" in it })
    }
}
