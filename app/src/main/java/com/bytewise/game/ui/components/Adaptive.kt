package com.bytewise.game.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** True when the window is wider than tall. The activity handles rotation itself, so this updates live. */
@Composable
fun isLandscape(): Boolean {
    val c = LocalConfiguration.current
    return c.screenWidthDp > c.screenHeightDp
}
