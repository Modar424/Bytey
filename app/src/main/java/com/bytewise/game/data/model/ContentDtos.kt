package com.bytewise.game.data.model

import kotlinx.serialization.Serializable

/**
 * One record of assets/questions/questions_<lang>.json (a JSON array of these).
 * The AR and EN files must contain the same ids, levels, categories, conceptIds,
 * correctIndex and difficulty; only text/options/explanation are translated.
 */
@Serializable
data class QuestionDto(
    val id: String,
    val level: Int,
    val category: QuestionCategory,
    val conceptId: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: Int,
)

@Serializable
data class ResourceDto(
    val url: String,
    val type: ResourceType,
    val language: ResourceLanguage,
    val title: String,
    val description: String,
)

/** One record of assets/concepts/concepts_<lang>.json. Resources are paired by index across languages. */
@Serializable
data class ConceptDto(
    val id: String,
    val name: String,
    val description: String,
    val resources: List<ResourceDto> = emptyList(),
    val practiceChallenge: String,
    val advancedChallenge: String? = null,
    val estimatedMinutes: Int,
)
