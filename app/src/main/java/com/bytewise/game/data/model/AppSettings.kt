package com.bytewise.game.data.model

enum class AppLanguage(val tag: String) {
    AR("ar"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: AR
    }
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.AR,
    val soundEnabled: Boolean = true,
    val byteyMuted: Boolean = false,
)
