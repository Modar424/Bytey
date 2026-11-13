package com.bytewise.game.data.model

import kotlinx.serialization.Serializable

/** The five permanent categories. JSON uses the enum names. */
@Serializable
enum class QuestionCategory { GENERAL, TOKENS, AI_TOOLS, MODELS, ARCHITECTURE }

@Serializable
enum class ResourceType { ARTICLE, VIDEO, DOCUMENTATION, PRACTICE, COURSE }

/** Language the linked page itself is written in. */
@Serializable
enum class ResourceLanguage { AR, EN }
