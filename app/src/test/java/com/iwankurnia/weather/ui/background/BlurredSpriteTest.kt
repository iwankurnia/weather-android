package com.iwankurnia.weather.ui.background

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurredSpriteTest {

    @Test
    fun boxSizes_matchTheGaussianVariance() {
        for (sigma in floatArrayOf(1.5f, 2.5f, 3f, 6f, 12f)) {
            val sizes = gaussianBoxSizes(sigma)
            assertEquals(3, sizes.size)
            sizes.forEach { assertTrue("odd width $it", it % 2 == 1) }
            // A box of width w has variance (w² − 1) / 12; the passes add up.
            val variance = sizes.sumOf { (it * it - 1) / 12.0 }
            val expected = sigma * sigma.toDouble()
            assertTrue("sigma $sigma: variance $variance", abs(variance - expected) / expected < 0.15)
        }
    }

    @Test
    fun blur_spreadsAndKeepsTheAlpha() {
        val size = 41
        val pixels = square(size, colour = 0xFF336699.toInt())
        val before = totalAlpha(pixels)
        blurArgb(pixels, size, size, 3f)
        val after = totalAlpha(pixels)
        assertTrue("alpha $before → $after", abs(after - before) / before < 0.03)
        // The centre stays solid-ish, the edges of the square soften, and the corner is still clear.
        assertTrue(alphaAt(pixels, size, 20, 20) > 200)
        assertTrue(alphaAt(pixels, size, 20, 14) in 1..254)
        assertEquals(0, alphaAt(pixels, size, 0, 0))
    }

    @Test
    fun blur_doesNotDarkenFadingEdges() {
        val size = 41
        val pixels = square(size, colour = 0xFF336699.toInt())
        blurArgb(pixels, size, size, 3f)
        for (c in pixels) {
            if ((c ushr 24) < 32) continue
            assertTrue(abs(((c shr 16) and 0xFF) - 0x33) <= 3)
            assertTrue(abs(((c shr 8) and 0xFF) - 0x66) <= 3)
            assertTrue(abs((c and 0xFF) - 0x99) <= 3)
        }
    }

    @Test
    fun blur_isSymmetric() {
        val size = 41
        val pixels = square(size, colour = 0xFFFFFFFF.toInt())
        blurArgb(pixels, size, size, 4f)
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Running sums round slightly differently in each direction: allow one step.
                val here = alphaAt(pixels, size, x, y)
                assertTrue(abs(here - alphaAt(pixels, size, size - 1 - x, y)) <= 1)
                assertTrue(abs(here - alphaAt(pixels, size, x, size - 1 - y)) <= 1)
            }
        }
    }

    /** A transparent field with an opaque 11 × 11 square in the middle. */
    private fun square(size: Int, colour: Int) = IntArray(size * size) { i ->
        val x = i % size
        val y = i / size
        if (x in 15..25 && y in 15..25) colour else 0
    }

    private fun alphaAt(pixels: IntArray, size: Int, x: Int, y: Int) = pixels[y * size + x] ushr 24

    private fun totalAlpha(pixels: IntArray) = pixels.sumOf { (it ushr 24).toDouble() }
}
