package com.bytewise.game.data.model

/** A localized question: both languages live in one record; options/answers are identical in order. */
data class Question(
    val id: String,
    val level: Int,
    val category: QuestionCategory,
    val conceptId: String,
    val textAr: String,
    val textEn: String,
    val optionsAr: List<String>,
    val optionsEn: List<String>,
    val correctIndex: Int,
    val explanationAr: String,
    val explanationEn: String,
    val difficulty: Int,
)

data class Resource(
    val titleAr: String,
    val titleEn: String,
    val url: String,
    val type: ResourceType,
    val language: ResourceLanguage,
    val descriptionAr: String,
    val descriptionEn: String,
)

data class Concept(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val resources: List<Resource>,
    val practiceChallengeAr: String,
    val practiceChallengeEn: String,
    val advancedChallengeAr: String? = null,
    val advancedChallengeEn: String? = null,
    val estimatedMinutes: Int,
)

fun Question.text(lang: AppLanguage) = if (lang == AppLanguage.AR) textAr else textEn
fun Question.options(lang: AppLanguage) = if (lang == AppLanguage.AR) optionsAr else optionsEn
fun Question.explanation(lang: AppLanguage) = if (lang == AppLanguage.AR) explanationAr else explanationEn

fun Concept.name(lang: AppLanguage) = if (lang == AppLanguage.AR) nameAr else nameEn
fun Concept.description(lang: AppLanguage) = if (lang == AppLanguage.AR) descriptionAr else descriptionEn
fun Concept.practiceChallenge(lang: AppLanguage) = if (lang == AppLanguage.AR) practiceChallengeAr else practiceChallengeEn
fun Concept.advancedChallenge(lang: AppLanguage) = if (lang == AppLanguage.AR) advancedChallengeAr else advancedChallengeEn

fun Resource.title(lang: AppLanguage) = if (lang == AppLanguage.AR) titleAr else titleEn
fun Resource.description(lang: AppLanguage) = if (lang == AppLanguage.AR) descriptionAr else descriptionEn
