package com.bytewise.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.ThemeMode
import com.bytewise.game.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { repo.setLanguage(language) }

    fun toggleLanguage() {
        val next = if (settings.value.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
        setLanguage(next)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }

    /** Quick toggle: flips whatever is currently displayed (light <-> dark). */
    fun quickToggleTheme(currentlyDark: Boolean) =
        setThemeMode(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK)

    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { repo.setSoundEnabled(enabled) }

    fun toggleSound() = setSoundEnabled(!settings.value.soundEnabled)

    fun setByteyMuted(muted: Boolean) = viewModelScope.launch { repo.setByteyMuted(muted) }

    fun toggleByteyMuted() = setByteyMuted(!settings.value.byteyMuted)
}
