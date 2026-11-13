package com.bytewise.game.bytey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Verifies the generated pixel art obeys the rules: equal 64x64 frames, hard alpha, small palette. */
class ByteySpriteAssetsTest {
    private val states = listOf(
        "idle", "happy", "excited", "super_speed", "sad", "surprised",
        "thinking", "laughing", "running", "celebrating", "level_unlocked", "learning_mode",
    )

    private fun root(): File =
        listOf(File("src/main/assets/bytey"), File("app/src/main/assets/bytey")).first { it.exists() }

    private fun readImage(file: File): Any {
        val image = Class.forName("javax.imageio.ImageIO")
            .getMethod("read", File::class.java)
            .invoke(null, file)
        requireNotNull(image) { "failed to decode ${file.path}" }
        return image
    }

    private fun width(image: Any): Int = image.javaClass.getMethod("getWidth").invoke(image) as Int
    private fun height(image: Any): Int = image.javaClass.getMethod("getHeight").invoke(image) as Int
    private fun rgb(image: Any, x: Int, y: Int): Int =
        image.javaClass.getMethod("getRGB", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .invoke(image, x, y) as Int

    @Test fun everyStateExistsForEveryTierWithValidPixelArt() {
        for (tier in 0..4) for (state in states) {
            val sheet = File(root(), "sprites/tier$tier/$state.png")
            val still = File(root(), "static/tier$tier/$state.png")
            assertTrue("missing ${sheet.path}", sheet.exists())
            assertTrue("missing ${still.path}", still.exists())

            val img = readImage(sheet)
            assertEquals("$state height", 64, height(img))
            assertEquals("$state width multiple of 64", 0, width(img) % 64)
            val colors = HashSet<Int>()
            var opaque = 0
            for (y in 0 until height(img)) for (x in 0 until width(img)) {
                val argb = rgb(img, x, y)
                val a = argb ushr 24
                assertTrue("$state tier$tier has anti-aliased alpha $a at $x,$y", a == 0 || a == 255)
                if (a == 255) { opaque++; colors.add(argb) }
            }
            assertTrue("$state tier$tier is empty", opaque > 200)
            assertTrue("$state tier$tier uses ${colors.size} colours (limit 45)", colors.size <= 45)

            val s = readImage(still)
            assertEquals(64, width(s)); assertEquals(64, height(s))
        }
    }

    @Test fun framesWithinASheetAreNotAllIdentical() {
        for (state in listOf("idle", "running", "celebrating")) {
            val img = readImage(File(root(), "sprites/tier0/$state.png"))
            val frames = width(img) / 64
            assertTrue(frames >= 4)
            fun frame(i: Int) = (0 until 64 * 64).map { rgb(img, i * 64 + it % 64, it / 64) }
            assertTrue("$state frames identical", (1 until frames).any { frame(it) != frame(0) })
        }
    }
}
