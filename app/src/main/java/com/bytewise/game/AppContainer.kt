package com.bytewise.game

import android.content.Context
import com.bytewise.game.data.local.BytewiseDatabase
import com.bytewise.game.data.repository.ContentRepository
import com.bytewise.game.data.repository.GameStateRepository
import com.bytewise.game.data.repository.ProgressRepository
import com.bytewise.game.data.repository.ReportRepository
import com.bytewise.game.data.repository.SessionRepository
import com.bytewise.game.data.repository.SettingsRepository
import com.bytewise.game.data.source.AssetContentSource

/** Manual dependency container (no DI library needed). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: BytewiseDatabase by lazy { BytewiseDatabase.getInstance(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val progressRepository: ProgressRepository by lazy { ProgressRepository(appContext) }
    val contentRepository: ContentRepository by lazy { ContentRepository(AssetContentSource(appContext)) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database, progressRepository) }
    val reportRepository: ReportRepository by lazy { ReportRepository(database) }
    val gameStateRepository: GameStateRepository by lazy { GameStateRepository(appContext) }
}
