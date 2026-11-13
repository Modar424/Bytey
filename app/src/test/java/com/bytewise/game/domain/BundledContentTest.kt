package com.bytewise.game.domain

import com.bytewise.game.data.model.ConceptDto
import com.bytewise.game.data.model.QuestionCategory
import com.bytewise.game.data.model.QuestionDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates the REAL bundled JSON (assets/questions, assets/concepts) with the production merger/validator. */
class BundledContentTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val arabic = Regex("[\\u0600-\\u06FF]")

    private fun assetFile(path: String): File =
        listOf("src/main/assets/$path", "app/src/main/assets/$path").map(::File).first { it.exists() }

    private fun <T> load(path: String, serializer: KSerializer<T>): List<T> =
        json.decodeFromString(ListSerializer(serializer), assetFile(path).readText(Charsets.UTF_8))

    private val qEn = load("questions/questions_en.json", QuestionDto.serializer())
    private val qAr = load("questions/questions_ar.json", QuestionDto.serializer())
    private val cEn = load("concepts/concepts_en.json", ConceptDto.serializer())
    private val cAr = load("concepts/concepts_ar.json", ConceptDto.serializer())

    private val concepts = ContentMerger.mergeConcepts(cEn, cAr)
    private val questions = ContentMerger.mergeQuestions(qEn, qAr)

    @Test fun languagesMergeWithoutAnyMismatch() {
        assertEquals(emptyList<String>(), concepts.problems)
        assertEquals(emptyList<String>(), questions.problems)
    }

    @Test fun fullBankIs250QuestionsFiveCategoriesPerLevel() {
        assertEquals(250, questions.items.size)
        assertEquals(emptyList<String>(), ContentValidator.validateFullBank(questions.items))
    }

    @Test fun questionsPassStructuralValidation() {
        val ids = concepts.items.map { it.id }.toSet()
        assertEquals(emptyList<String>(), ContentValidator.validateQuestions(questions.items, ids))
    }

    @Test fun atLeast25ConceptsEachWithThreeToFiveResources() {
        assertEquals(emptyList<String>(), ContentValidator.validateConcepts(concepts.items, minConcepts = 25))
        for (c in concepts.items) assertTrue("${c.id} resources=${c.resources.size}", c.resources.size in 3..5)
    }

    @Test fun everyConceptHasQuestionsAndEveryQuestionHasAConcept() {
        val used = questions.items.map { it.conceptId }.toSet()
        assertEquals(emptySet<String>(), concepts.items.map { it.id }.toSet() - used)
    }

    @Test fun languageIsolation() {
        for (q in questions.items) {
            assertTrue("${q.id} EN has Arabic", !arabic.containsMatchIn(q.textEn + q.optionsEn.joinToString() + q.explanationEn))
            assertTrue("${q.id} AR text lacks Arabic", arabic.containsMatchIn(q.textAr) && arabic.containsMatchIn(q.explanationAr))
        }
        for (c in concepts.items) {
            assertTrue("${c.id} EN has Arabic", !arabic.containsMatchIn(c.nameEn + c.descriptionEn + c.practiceChallengeEn))
            assertTrue("${c.id} AR lacks Arabic", arabic.containsMatchIn(c.nameAr + c.descriptionAr))
        }
    }

    @Test fun correctAnswerPositionIsBalanced() {
        val counts = questions.items.groupingBy { it.correctIndex }.eachCount()
        for (i in 0..3) assertTrue("index $i count=${counts[i]}", (counts[i] ?: 0) in 40..85)
    }

    @Test fun everyLevelCategoryHasFiveQuestions() {
        for (level in 1..10) for (cat in QuestionCategory.entries) {
            assertEquals(5, questions.items.count { it.level == level && it.category == cat })
        }
    }
}
