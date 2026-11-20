package com.bytewise.game.ui.bytey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bytewise.game.R
import kotlinx.coroutines.delay

/**
 * Draws one animation state of Bytey. Frames are cut from the sheet and drawn with nearest-neighbour
 * filtering at an integer scale. With system animations disabled it shows the static key frame.
 */
@Composable
fun ByteySprite(
    state: ByteyState,
    tier: Int,
    spriteSize: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduced = remember { ByteySprites.isReducedMotion(context) }
    val sheet by produceState<ImageBitmap?>(null, tier, state, reduced) {
        value = ByteySprites.load(context, tier, state, static = reduced)
    }
    var frame by remember(state, tier) { mutableIntStateOf(0) }

    LaunchedEffect(sheet, state, reduced) {
        val image = sheet ?: return@LaunchedEffect
        val frames = image.width / ByteySprites.FRAME
        frame = 0
        if (reduced || frames <= 1) return@LaunchedEffect
        val stepMs = 1000L / state.fps
        while (true) {
            delay(stepMs)
            if (frame < frames - 1) frame++ else if (state.loop) frame = 0 else break
        }
    }

    val description = stringResource(R.string.cd_bytey)
    Canvas(modifier.size(spriteSize).semantics { contentDescription = description }) {
        val image = sheet ?: return@Canvas
        val side = ByteySprites.FRAME
        val scale = (minOf(this.size.width, this.size.height) / side).toInt().coerceAtLeast(1)
        val dst = side * scale
        val ox = ((this.size.width - dst) / 2f).toInt()
        val oy = (this.size.height - dst).toInt() // feet stay on the bottom edge
        val srcX = (if (reduced) 0 else frame.coerceAtMost(image.width / side - 1)) * side
        drawImage(
            image = image,
            srcOffset = IntOffset(srcX, 0),
            srcSize = IntSize(side, side),
            dstOffset = IntOffset(ox, oy),
            dstSize = IntSize(dst, dst),
            filterQuality = FilterQuality.None,
        )
    }
}
