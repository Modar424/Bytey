package com.bytewise.game.bytey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

/** Verifies the generated pixel art obeys the rules: equal 64x64 frames, hard alpha, small palette. */
class ByteySpriteAssetsTest {
    private val states = listOf(
        "idle", "happy", "excited", "super_speed", "sad", "surprised",
        "thinking", "laughing", "running", "celebrating", "level_unlocked", "learning_mode",
    )

    private fun root(): File =
        listOf(File("src/main/assets/bytey"), File("app/src/main/assets/bytey")).first { it.exists() }

    @Test fun everyStateExistsForEveryTierWithValidPixelArt() {
        for (tier in 0..4) for (state in states) {
            val sheet = File(root(), "sprites/tier$tier/$state.png")
            val still = File(root(), "static/tier$tier/$state.png")
            assertTrue("missing ${sheet.path}", sheet.exists())
            assertTrue("missing ${still.path}", still.exists())

            val img = ImageIO.read(sheet)
            assertEquals("$state height", 64, img.height)
            assertEquals("$state width multiple of 64", 0, img.width % 64)
            val colors = HashSet<Int>()
            var opaque = 0
            for (y in 0 until img.height) for (x in 0 until img.width) {
                val argb = img.getRGB(x, y)
                val a = argb ushr 24
                assertTrue("$state tier$tier has anti-aliased alpha $a at $x,$y", a == 0 || a == 255)
                if (a == 255) { opaque++; colors.add(argb) }
            }
            assertTrue("$state tier$tier is empty", opaque > 200)
            assertTrue("$state tier$tier uses ${colors.size} colours (limit 45)", colors.size <= 45)

            val s = ImageIO.read(still)
            assertEquals(64, s.width); assertEquals(64, s.height)
        }
    }

    @Test fun framesWithinASheetAreNotAllIdentical() {
        for (state in listOf("idle", "running", "celebrating")) {
            val img = ImageIO.read(File(root(), "sprites/tier0/$state.png"))
            val frames = img.width / 64
            assertTrue(frames >= 4)
            fun frame(i: Int) = (0 until 64 * 64).map { img.getRGB(i * 64 + it % 64, it / 64) }
            assertTrue("$state frames identical", (1 until frames).any { frame(it) != frame(0) })
        }
    }
}
