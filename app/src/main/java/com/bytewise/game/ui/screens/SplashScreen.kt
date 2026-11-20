package com.bytewise.game.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.R
import com.bytewise.game.ui.bytey.ByteySprite
import com.bytewise.game.ui.bytey.ByteyState
import com.bytewise.game.ui.components.GameTitle
import com.bytewise.game.ui.components.MenuBackdrop
import com.bytewise.game.ui.components.isLandscape
import com.bytewise.game.ui.theme.PixelFontFamily
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(byteyTier: Int, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onFinished()
    }
    val landscape = isLandscape()
    Box(Modifier.fillMaxSize()) {
        MenuBackdrop()
        GameTitle(
            Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = if (landscape) 8.dp else 56.dp),
            compact = landscape,
        )
        ByteySprite(
            state = ByteyState.RUNNING,
            tier = byteyTier,
            spriteSize = if (landscape) 96.dp else 140.dp,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = stringResource(R.string.splash_loading),
            color = Color.White,
            fontFamily = PixelFontFamily,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = if (landscape) 12.dp else 32.dp),
        )
    }
}
