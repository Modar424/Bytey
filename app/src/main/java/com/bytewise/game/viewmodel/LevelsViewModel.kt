package com.bytewise.game.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bytewise.game.BytewiseApplication
import com.bytewise.game.domain.GameConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LevelsViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as BytewiseApplication).container

    private val _levelCount = MutableStateFlow(GameConfig.INITIAL_LEVEL_COUNT)
    val levelCount: StateFlow<Int> = _levelCount.asStateFlow()

    /** Level of the run that was left unfinished, or null. */
    private val _unfinishedLevel = MutableStateFlow<Int?>(null)
    val unfinishedLevel: StateFlow<Int?> = _unfinishedLevel.asStateFlow()

    init {
        viewModelScope.launch { _levelCount.value = container.contentRepository.levelCount() }
        refreshUnfinished()
    }

    fun refreshUnfinished() {
        viewModelScope.launch {
            val saved = container.gameStateRepository.load()
            val session = saved?.let { container.sessionRepository.getSession(it.sessionId) }
            _unfinishedLevel.value = if (session != null && session.completedAt == null) session.level else null
        }
    }
}
