package com.bytewise.game.data.repository

import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.Concept
import com.bytewise.game.data.model.Question
import com.bytewise.game.data.source.AssetContentSource
import com.bytewise.game.domain.ContentMerger
import com.bytewise.game.domain.ContentValidator
import com.bytewise.game.domain.GameConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException

data class ContentBundle(
    val questions: List<Question>,
    val concepts: List<Concept>,
    /** Load/merge/validation problems; empty when the bank is consistent. */
    val problems: List<String>,
)

class ContentRepository(private val source: AssetContentSource) {
    private val mutex = Mutex()
    private var cached: ContentBundle? = null

    suspend fun bundle(): ContentBundle = mutex.withLock { cached ?: load().also { cached = it } }

    suspend fun questionsForLevel(level: Int): List<Question> = bundle().questions.filter { it.level == level }

    suspend fun conceptById(id: String): Concept? = bundle().concepts.firstOrNull { it.id == id }

    /** Number of levels present in the question bank (new levels need only new JSON records). */
    suspend fun levelCount(): Int =
        bundle().questions.maxOfOrNull { it.level } ?: GameConfig.INITIAL_LEVEL_COUNT

    private suspend fun load(): ContentBundle {
        val problems = mutableListOf<String>()
        return try {
            val qEn = source.loadQuestions(AppLanguage.EN)
            val qAr = source.loadQuestions(AppLanguage.AR)
            val cEn = source.loadConcepts(AppLanguage.EN)
            val cAr = source.loadConcepts(AppLanguage.AR)
            if (qEn == null) problems += "questions_en.json not found"
            if (qAr == null) problems += "questions_ar.json not found"
            if (cEn == null) problems += "concepts_en.json not found"
            if (cAr == null) problems += "concepts_ar.json not found"

            val concepts = ContentMerger.mergeConcepts(cEn.orEmpty(), cAr.orEmpty())
            val questions = ContentMerger.mergeQuestions(qEn.orEmpty(), qAr.orEmpty())
            problems += concepts.problems
            problems += questions.problems
            problems += ContentValidator.validateConcepts(concepts.items)
            problems += ContentValidator.validateQuestions(questions.items, concepts.items.map { it.id }.toSet())
            ContentBundle(questions.items, concepts.items, problems)
        } catch (e: SerializationException) {
            ContentBundle(emptyList(), emptyList(), problems + "Invalid JSON: ${e.message}")
        } catch (e: IllegalArgumentException) {
            ContentBundle(emptyList(), emptyList(), problems + "Invalid JSON: ${e.message}")
        }
    }
}
