package com.bytewise.game.domain

import com.bytewise.game.data.model.Concept
import com.bytewise.game.data.model.Question
import com.bytewise.game.data.model.QuestionCategory

/** Pure validation of the question/concept bank; returns human-readable problems (empty = valid). */
object ContentValidator {

    fun validateQuestions(questions: List<Question>, conceptIds: Set<String>): List<String> {
        val problems = mutableListOf<String>()
        if (questions.map { it.id }.toSet().size != questions.size) problems += "Duplicate question ids"
        val seenTexts = HashSet<String>()
        for (q in questions) {
            val id = q.id
            if (q.optionsEn.size != GameConfig.OPTION_COUNT || q.optionsAr.size != GameConfig.OPTION_COUNT) {
                problems += "$id: must have exactly ${GameConfig.OPTION_COUNT} options in both languages"
            }
            if (q.correctIndex !in 0 until GameConfig.OPTION_COUNT) problems += "$id: correctIndex out of range"
            if (q.optionsEn.toSet().size != q.optionsEn.size) problems += "$id: duplicate English options"
            if (q.optionsAr.toSet().size != q.optionsAr.size) problems += "$id: duplicate Arabic options"
            if (listOf(q.textAr, q.textEn, q.explanationAr, q.explanationEn).any { it.isBlank() } ||
                (q.optionsAr + q.optionsEn).any { it.isBlank() }
            ) {
                problems += "$id: blank text, option or explanation"
            }
            if (q.difficulty !in GameConfig.difficultyRange(q.level)) {
                problems += "$id: difficulty ${q.difficulty} outside the range for level ${q.level}"
            }
            if (conceptIds.isNotEmpty() && q.conceptId !in conceptIds) {
                problems += "$id: unknown conceptId '${q.conceptId}'"
            }
            if (!seenTexts.add(q.textEn.trim().lowercase())) problems += "$id: duplicate English question text"
        }
        return problems
    }

    /** The full initial bank: [levels] levels x 25 questions, 5 per category. */
    fun validateFullBank(questions: List<Question>, levels: Int = GameConfig.INITIAL_LEVEL_COUNT): List<String> {
        val problems = mutableListOf<String>()
        val expected = levels * GameConfig.QUESTIONS_PER_LEVEL
        if (questions.size != expected) problems += "Expected $expected questions, found ${questions.size}"
        for (level in 1..levels) {
            val inLevel = questions.filter { it.level == level }
            if (inLevel.size != GameConfig.QUESTIONS_PER_LEVEL) {
                problems += "Level $level has ${inLevel.size} questions (expected ${GameConfig.QUESTIONS_PER_LEVEL})"
            }
            for (c in QuestionCategory.entries) {
                val n = inLevel.count { it.category == c }
                if (n != GameConfig.QUESTIONS_PER_CATEGORY) {
                    problems += "Level $level category $c has $n questions (expected ${GameConfig.QUESTIONS_PER_CATEGORY})"
                }
            }
        }
        return problems
    }

    fun validateConcepts(concepts: List<Concept>, minConcepts: Int = 0): List<String> {
        val problems = mutableListOf<String>()
        if (concepts.size < minConcepts) problems += "Expected at least $minConcepts concepts, found ${concepts.size}"
        if (concepts.map { it.id }.toSet().size != concepts.size) problems += "Duplicate concept ids"
        for (c in concepts) {
            if (c.estimatedMinutes <= 0) problems += "${c.id}: estimatedMinutes must be positive"
            if (c.resources.size > GameConfig.MAX_CONCEPT_RESOURCES) problems += "${c.id}: more than ${GameConfig.MAX_CONCEPT_RESOURCES} resources"
            if (listOf(c.nameAr, c.nameEn, c.descriptionAr, c.descriptionEn, c.practiceChallengeAr, c.practiceChallengeEn).any { it.isBlank() }) {
                problems += "${c.id}: blank name, description or challenge"
            }
            for (r in c.resources) {
                if (!r.url.startsWith("https://")) problems += "${c.id}: resource URL must start with https:// (${r.url})"
                if (r.titleAr.isBlank() || r.titleEn.isBlank()) problems += "${c.id}: resource with blank title"
            }
        }
        return problems
    }
}
