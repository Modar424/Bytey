package com.bytewise.game.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bytewise.game.data.model.AppSettings
import com.bytewise.game.data.model.ProgressState
import com.bytewise.game.ui.screens.GameScreen
import com.bytewise.game.ui.screens.LearningReportScreen
import com.bytewise.game.ui.screens.ProgressScreen
import com.bytewise.game.ui.screens.LevelsScreen
import com.bytewise.game.ui.screens.MainMenuScreen
import com.bytewise.game.ui.screens.ResultScreen
import com.bytewise.game.ui.screens.SettingsScreen
import com.bytewise.game.ui.screens.SplashScreen
import com.bytewise.game.viewmodel.GameArgs
import com.bytewise.game.viewmodel.SettingsViewModel

object Routes {
    const val SPLASH = "splash"
    const val MENU = "menu"
    const val LEVELS = "levels"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val REPORT = "report/{${GameArgs.SESSION_ID}}"
    const val GAME = "game/{${GameArgs.LEVEL}}/{${GameArgs.MODE}}"
    const val RESULT = "result/{${GameArgs.SESSION_ID}}/{${GameArgs.NEW_BEST}}/{${GameArgs.UNLOCKED}}"

    fun report(sessionId: Long) = "report/$sessionId"
    fun game(level: Int, mode: String) = "game/$level/$mode"
    fun result(sessionId: Long, newBest: Boolean, unlockedLevel: Int) = "result/$sessionId/$newBest/$unlockedLevel"
}

@Composable
fun BytewiseNavHost(
    settings: AppSettings,
    isDark: Boolean,
    byteyTier: Int,
    progress: ProgressState,
    viewModel: SettingsViewModel,
    onQuit: () -> Unit,
) {
    val nav = rememberNavController()
    // Plain cross-fades: direction-neutral (works for RTL and LTR) and skipped by the system "remove animations" setting.
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(byteyTier = byteyTier, onFinished = {
                nav.navigate(Routes.MENU) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.MENU) {
            MainMenuScreen(
                settings = settings,
                isDark = isDark,
                byteyTier = byteyTier,
                // Resumes the unfinished run if there is one, otherwise starts the next level to play.
                onStart = { nav.navigate(Routes.game(GameArgs.AUTO_LEVEL, GameArgs.MODE_AUTO)) },
                onLevels = { nav.navigate(Routes.LEVELS) },
                onProgress = { nav.navigate(Routes.PROGRESS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onQuit = onQuit,
                onToggleSound = viewModel::toggleSound,
                onToggleLanguage = viewModel::toggleLanguage,
                onToggleTheme = { viewModel.quickToggleTheme(isDark) },
                onToggleByteyMute = viewModel::toggleByteyMuted,
            )
        }
        composable(Routes.LEVELS) {
            LevelsScreen(
                progress = progress,
                byteyTier = byteyTier,
                language = settings.language,
                onBack = { nav.popBackStack() },
                onPlay = { level, resume ->
                    nav.navigate(Routes.game(level, if (resume) GameArgs.MODE_RESUME else GameArgs.MODE_NEW))
                },
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(
                navArgument(GameArgs.LEVEL) { type = NavType.IntType },
                navArgument(GameArgs.MODE) { type = NavType.StringType },
            ),
        ) {
            GameScreen(
                settings = settings,
                byteyTier = byteyTier,
                onExit = { nav.popBackStack() },
                onFinished = { r ->
                    nav.navigate(Routes.result(r.sessionId, r.newBest, r.unlockedLevel)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument(GameArgs.SESSION_ID) { type = NavType.LongType },
                navArgument(GameArgs.NEW_BEST) { type = NavType.BoolType },
                navArgument(GameArgs.UNLOCKED) { type = NavType.IntType },
            ),
        ) {
            ResultScreen(
                settings = settings,
                byteyTier = byteyTier,
                onNextLevel = { level ->
                    nav.navigate(Routes.game(level, GameArgs.MODE_NEW)) { popUpTo(Routes.MENU) }
                },
                onRetry = { level ->
                    nav.navigate(Routes.game(level, GameArgs.MODE_NEW)) { popUpTo(Routes.MENU) }
                },
                onReport = { id -> nav.navigate(Routes.report(id)) },
                onMenu = { nav.popBackStack(Routes.MENU, false) },
            )
        }
        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument(GameArgs.SESSION_ID) { type = NavType.LongType }),
        ) {
            LearningReportScreen(settings = settings, byteyTier = byteyTier, onBack = { nav.popBackStack() })
        }
        composable(Routes.PROGRESS) {
            ProgressScreen(settings = settings, byteyTier = byteyTier, progress = progress, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                byteyTier = byteyTier,
                onBack = { nav.popBackStack() },
                onLanguage = viewModel::setLanguage,
                onTheme = viewModel::setThemeMode,
                onSound = viewModel::setSoundEnabled,
                onByteyMuted = viewModel::setByteyMuted,
            )
        }
    }
}
