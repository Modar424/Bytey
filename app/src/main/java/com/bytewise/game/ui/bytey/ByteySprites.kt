package com.bytewise.game.ui.bytey

import android.content.Context
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Loads the generated sprite sheets from assets/bytey (no scaling, no filtering) and caches them. */
object ByteySprites {
    const val FRAME = 64
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    suspend fun load(context: Context, tier: Int, state: ByteyState, static: Boolean): ImageBitmap? {
        val folder = if (static) "static" else "sprites"
        val path = "bytey/$folder/tier$tier/${state.assetName}.png"
        cache[path]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                context.applicationContext.assets.open(path).use { stream ->
                    val options = BitmapFactory.Options().apply { inScaled = false }
                    BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                }?.also { cache[path] = it }
            } catch (e: IOException) {
                null
            }
        }
    }

    /** True when the user disabled animations system-wide (animator duration scale = 0). */
    fun isReducedMotion(context: Context): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}
