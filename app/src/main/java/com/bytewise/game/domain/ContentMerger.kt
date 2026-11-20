package com.bytewise.game.domain

import com.bytewise.game.data.model.Concept
import com.bytewise.game.data.model.ConceptDto
import com.bytewise.game.data.model.Question
import com.bytewise.game.data.model.QuestionDto
import com.bytewise.game.data.model.Resource

data class MergeResult<T>(val items: List<T>, val problems: List<String>)

/** Combines the per-language JSON files into single localized records and reports every mismatch. */
object ContentMerger {

    fun mergeQuestions(en: List<QuestionDto>, ar: List<QuestionDto>): MergeResult<Question> {
        val problems = mutableListOf<String>()
        val items = mutableListOf<Question>()
        if (en.map { it.id }.toSet().size != en.size) problems += "questions_en.json has duplicate ids"
        if (ar.map { it.id }.toSet().size != ar.size) problems += "questions_ar.json has duplicate ids"
        val arById = ar.associateBy { it.id }
        val enIds = en.map { it.id }.toSet()
        for (e in en) {
            val a = arById[e.id]
            if (a == null) {
                problems += "Question ${e.id} exists in English but not in Arabic"
                continue
            }
            if (e.level != a.level || e.category != a.category || e.conceptId != a.conceptId ||
                e.difficulty != a.difficulty
            ) {
                problems += "Question ${e.id}: level/category/concept/difficulty differ between languages"
            }
            if (e.correctIndex != a.correctIndex) {
                problems += "Question ${e.id}: correctIndex differs between languages"
            }
            if (e.options.size != a.options.size) {
                problems += "Question ${e.id}: option count differs between languages"
            }
            items += Question(
                id = e.id,
                level = e.level,
                category = e.category,
                conceptId = e.conceptId,
                textAr = a.text,
                textEn = e.text,
                optionsAr = a.options,
                optionsEn = e.options,
                correctIndex = e.correctIndex,
                explanationAr = a.explanation,
                explanationEn = e.explanation,
                difficulty = e.difficulty,
            )
        }
        for (a in ar) if (a.id !in enIds) problems += "Question ${a.id} exists in Arabic but not in English"
        return MergeResult(items, problems)
    }

    fun mergeConcepts(en: List<ConceptDto>, ar: List<ConceptDto>): MergeResult<Concept> {
        val problems = mutableListOf<String>()
        val items = mutableListOf<Concept>()
        if (en.map { it.id }.toSet().size != en.size) problems += "concepts_en.json has duplicate ids"
        val arById = ar.associateBy { it.id }
        val enIds = en.map { it.id }.toSet()
        for (e in en) {
            val a = arById[e.id]
            if (a == null) {
                problems += "Concept ${e.id} exists in English but not in Arabic"
                continue
            }
            if (e.resources.size != a.resources.size) {
                problems += "Concept ${e.id}: resource count differs between languages"
            }
            val resources = mutableListOf<Resource>()
            for (i in 0 until minOf(e.resources.size, a.resources.size)) {
                val re = e.resources[i]
                val ra = a.resources[i]
                if (re.url != ra.url || re.type != ra.type || re.language != ra.language) {
                    problems += "Concept ${e.id}: resource #$i differs (url/type/language) between languages"
                }
                resources += Resource(
                    titleAr = ra.title,
                    titleEn = re.title,
                    url = re.url,
                    type = re.type,
                    language = re.language,
                    descriptionAr = ra.description,
                    descriptionEn = re.description,
                )
            }
            items += Concept(
                id = e.id,
                nameAr = a.name,
                nameEn = e.name,
                descriptionAr = a.description,
                descriptionEn = e.description,
                resources = resources,
                practiceChallengeAr = a.practiceChallenge,
                practiceChallengeEn = e.practiceChallenge,
                advancedChallengeAr = a.advancedChallenge,
                advancedChallengeEn = e.advancedChallenge,
                estimatedMinutes = e.estimatedMinutes,
            )
        }
        for (a in ar) if (a.id !in enIds) problems += "Concept ${a.id} exists in Arabic but not in English"
        return MergeResult(items, problems)
    }
}
