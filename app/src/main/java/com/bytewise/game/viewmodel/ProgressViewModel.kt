package com.bytewise.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.data.model.ProgressState
import com.bytewise.game.domain.ByteyCosmetics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as BytewiseApplication).container.progressRepository

    val progress: StateFlow<ProgressState> =
        repo.progress.stateIn(viewModelScope, SharingStarted.Eagerly, ProgressState())

    /** Bytey's cosmetic tier follows the highest unlocked level. */
    val byteyTier: StateFlow<Int> = progress
        .map { ByteyCosmetics.tierForMaxLevel(it.unlockedLevels.maxOrNull() ?: 1) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
