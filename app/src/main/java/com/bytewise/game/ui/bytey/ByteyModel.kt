package com.bytewise.game.ui.bytey

import androidx.annotation.ArrayRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bytewise.game.R

/** The 12 animation states; [assetName] matches the generated sprite sheet file name. */
enum class ByteyState(val assetName: String, val fps: Int, val loop: Boolean, val keyFrame: Int = 0) {
    IDLE("idle", 4, true),
    HAPPY("happy", 8, true),
    EXCITED("excited", 8, true),
    SUPER_SPEED("super_speed", 12, true),
    SAD("sad", 5, true),
    SURPRISED("surprised", 6, false),
    THINKING("thinking", 3, true),
    LAUGHING("laughing", 10, true),
    RUNNING("running", 10, true),
    CELEBRATING("celebrating", 8, true),
    LEVEL_UNLOCKED("level_unlocked", 7, true),
    LEARNING_MODE("learning_mode", 4, true),
}

/** Display sizes. The sprite is always drawn at an integer multiple of 64 px so pixels stay crisp. */
enum class ByteyDisplayMode(val size: Dp) {
    NORMAL(88.dp),
    INTERACTION(140.dp),
    CELEBRATION(220.dp),
}

/** Events Bytey reacts to. Each draws its message from a localized string-array (random, no immediate repeat). */
enum class ByteyEvent(
    val state: ByteyState,
    val mode: ByteyDisplayMode,
    val holdMs: Long,
    @ArrayRes val messages: Int,
) {
    CORRECT(ByteyState.HAPPY, ByteyDisplayMode.INTERACTION, 2800, R.array.bytey_correct),
    FAST_CORRECT(ByteyState.SUPER_SPEED, ByteyDisplayMode.INTERACTION, 2800, R.array.bytey_fast),
    INCORRECT(ByteyState.SAD, ByteyDisplayMode.INTERACTION, 3000, R.array.bytey_incorrect),
    TIMEOUT(ByteyState.SURPRISED, ByteyDisplayMode.INTERACTION, 3000, R.array.bytey_timeout),
    STREAK(ByteyState.EXCITED, ByteyDisplayMode.CELEBRATION, 3200, R.array.bytey_streak),
    PERSONAL_BEST(ByteyState.CELEBRATING, ByteyDisplayMode.CELEBRATION, 3800, R.array.bytey_personal_best),
    LEVEL_COMPLETE_HIGH(ByteyState.CELEBRATING, ByteyDisplayMode.CELEBRATION, 4000, R.array.bytey_level_high),
    LEVEL_COMPLETE_AVERAGE(ByteyState.HAPPY, ByteyDisplayMode.INTERACTION, 3500, R.array.bytey_level_average),
    LEVEL_COMPLETE_LOW(ByteyState.SAD, ByteyDisplayMode.INTERACTION, 3500, R.array.bytey_level_low),
    LEVEL_UNLOCKED(ByteyState.LEVEL_UNLOCKED, ByteyDisplayMode.CELEBRATION, 4000, R.array.bytey_level_unlocked),
    LEARNING_REPORT(ByteyState.LEARNING_MODE, ByteyDisplayMode.INTERACTION, 4000, R.array.bytey_learning_report),
    IMPROVEMENT(ByteyState.CELEBRATING, ByteyDisplayMode.CELEBRATION, 3800, R.array.bytey_improvement),
    MENU_GREETING(ByteyState.HAPPY, ByteyDisplayMode.NORMAL, 4200, R.array.bytey_menu_greeting),
    SETTINGS_TIP(ByteyState.IDLE, ByteyDisplayMode.NORMAL, 4500, R.array.bytey_settings_tip),
    LEVELS_TIP(ByteyState.HAPPY, ByteyDisplayMode.NORMAL, 4500, R.array.bytey_levels_tip),
    COMING_SOON(ByteyState.THINKING, ByteyDisplayMode.INTERACTION, 60_000, R.array.bytey_coming_soon),
}
