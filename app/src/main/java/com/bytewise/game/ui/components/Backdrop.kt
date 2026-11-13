package com.bytewise.game.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytewise.game.R
import com.bytewise.game.ui.theme.PixelFontFamily

/** Full-screen artwork background (portrait crop of the supplied artwork) with readability scrims. */
@Composable
fun MenuBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.menu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x990F172A),
                        0.25f to Color.Transparent,
                        0.60f to Color.Transparent,
                        1f to Color(0xEE0F172A),
                    ),
                ),
        )
    }
}

/** Localized game title + tagline drawn natively (replaces the baked-in artwork text). */
@Composable
fun GameTitle(modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.app_title),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 28.sp else 40.sp,
                color = Color(0xFFFBBF24),
                shadow = Shadow(Color(0xFF1E1B4B), Offset(6f, 6f), 0f),
            ),
        )
        Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        Text(
            text = stringResource(R.string.tagline),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color(0xB30F172A))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            style = TextStyle(
                fontFamily = PixelFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White,
            ),
        )
    }
}
