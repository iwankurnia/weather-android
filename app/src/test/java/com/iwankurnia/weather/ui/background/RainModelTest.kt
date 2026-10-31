package com.iwankurnia.weather.ui.background

import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RainModelTest {

    private val width = 576f
    private val height = 1040f

    private fun model(storm: Boolean, animated: Boolean = true, seed: Int = 7) =
        RainModel(width, height, storm = storm, animated = animated, random = Random(seed))

    @Test
    fun drops_recycleAtBottom() {
        val rain = model(storm = false)
        repeat(600) { rain.tick(FRAME_MS) }
        for (i in 0 until rain.dropCount) {
            val y = rain.dropY[i]
            assertTrue("drop $i at y=$y above its length", y >= -rain.dropLen[i])
            assertTrue("drop $i at y=$y below the bottom", y <= height)
        }
    }

    @Test
    fun storm_hasMoreDropsAndWind() {
        val rain = model(storm = false)
        val storm = model(storm = true)
        assertTrue(storm.dropCount > rain.dropCount)
        assertTrue(storm.wind > rain.wind)
    }

    @Test
    fun bolt_staysInUpperHalf() {
        val storm = model(storm = true, seed = 42)
        val limit = 0.6f * height
        var lastStrike = 0
        var pointsChecked = 0
        // About two minutes of frames: plenty of strikes at one every 3.5–7.5 s.
        repeat(7_200) {
            storm.tick(FRAME_MS)
            if (storm.strikeCount != lastStrike) {
                lastStrike = storm.strikeCount
                for (i in 0 until storm.boltSize) {
                    assertTrue("bolt point y=${storm.boltY[i]}", storm.boltY[i] <= limit)
                    pointsChecked++
                }
                for (b in 0 until storm.branchCount) {
                    for (j in 0 until storm.branchSize[b]) {
                        val y = storm.branchY[b * RainModel.BRANCH_POINTS + j]
                        assertTrue("branch point y=$y", y <= limit)
                        pointsChecked++
                    }
                }
            }
        }
        assertTrue("expected several strikes, got $lastStrike", lastStrike >= 10)
        assertTrue(pointsChecked > 0)
    }

    @Test
    fun reducedMotion_staticFrame() {
        val still = model(storm = true, animated = false)
        val xs = still.dropX.copyOf()
        val ys = still.dropY.copyOf()
        repeat(500) { still.tick(FRAME_MS) }
        assertArrayEquals(xs, still.dropX, 0f)
        assertArrayEquals(ys, still.dropY, 0f)
        assertEquals(0, still.strikeCount)
        assertEquals(0f, still.flash, 0f)
        assertEquals(0f, still.boltAlpha, 0f)
        assertEquals(0, still.activeSplashes)
    }

    @Test
    fun reducedMotion_showsFewerDrops() {
        val animated = model(storm = false)
        val still = model(storm = false, animated = false)
        assertTrue(still.dropCount in 1 until animated.dropCount)
    }

    @Test
    fun nearDrops_leaveSplashesAlongTheBottom() {
        val rain = model(storm = false)
        var seen = 0
        repeat(300) {
            rain.tick(FRAME_MS)
            for (i in 0 until RainModel.SPLASH_CAPACITY) {
                if (rain.splashAlpha[i] > 0f) {
                    assertTrue(rain.splashY[i] >= height * 0.94f)
                    seen++
                }
            }
        }
        assertTrue(seen > 0)
    }

    @Test
    fun flash_decaysAfterStrike() {
        val storm = model(storm = true, seed = 3)
        while (storm.strikeCount == 0) storm.tick(FRAME_MS)
        val first = storm.flash
        assertTrue(first > 0f)
        repeat(60) { storm.tick(FRAME_MS) }
        assertTrue(storm.flash < first)
    }

    @Test
    fun boltAlpha_dipsOnlyTheChosenNumberOfTimes() {
        for (flickers in 1..2) {
            var life = 1f
            var previous = 1f
            var dips = 0
            while (life > 0f) {
                val alpha = RainModel.boltAlphaFor(life, flickers)
                if (alpha == RainModel.FLICKER_ALPHA && previous == 1f) dips++
                if (life <= 0.6f) assertTrue("fades after the fresh phase", alpha <= previous || previous == 1f)
                previous = alpha
                life -= 0.025f
            }
            assertEquals(flickers, dips)
        }
        assertEquals(0f, RainModel.boltAlphaFor(0f, 1), 0f)
    }

    @Test
    fun bolt_flickersOnceOrTwicePerStrike() {
        val storm = model(storm = true, seed = 11)
        val seen = mutableSetOf<Int>()
        var strike = 0
        var flickers = 0
        var dips = 0
        var previous = 0f
        // About two minutes of frames.
        repeat(7_200) {
            storm.tick(FRAME_MS)
            if (storm.strikeCount != strike) {
                if (strike > 0) {
                    assertEquals("strike $strike", flickers, dips)
                    seen += dips
                }
                strike = storm.strikeCount
                flickers = storm.boltFlickers
                dips = 0
            }
            val alpha = storm.boltAlpha
            if (alpha == RainModel.FLICKER_ALPHA && previous == 1f) dips++
            previous = alpha
        }
        assertTrue("expected several strikes, got $strike", strike >= 10)
        assertEquals(setOf(1, 2), seen)
    }

    @Test
    fun bolt_flickerIsRepeatableForASeed() {
        val a = model(storm = true, seed = 5)
        val b = model(storm = true, seed = 5)
        repeat(1_500) {
            a.tick(FRAME_MS)
            b.tick(FRAME_MS)
            assertEquals(a.boltAlpha, b.boltAlpha, 0f)
        }
    }

    private companion object {
        const val FRAME_MS = 16.7f
    }
}
