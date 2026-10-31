package com.iwankurnia.weather.ui.background

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Pure simulation of the falling-rain background, ported from `mockups/rain-storm.html`.
 *
 * Positions are in pixels. [unit] is the size in pixels of one mockup canvas pixel; the mockup
 * draws at twice its CSS size, so on a phone this is half a dp. Every number below is the
 * mockup's own, multiplied by [unit]. State lives in reused arrays so [tick] never allocates.
 *
 * With [animated] false the model holds one still frame: fewer drops, no splashes, no lightning.
 */
class RainModel(
    val width: Float,
    val height: Float,
    val storm: Boolean,
    val animated: Boolean = true,
    private val unit: Float = 1f,
    private val random: Random = Random.Default,
) {
    /** Horizontal travel per pixel of fall. Storms slant more steeply. */
    val wind: Float = if (storm) 0.28f else 0.1f

    val dropCount: Int = dropCountFor(width, height, unit, storm, animated)
    val dropX = FloatArray(dropCount)
    val dropY = FloatArray(dropCount)
    val dropLen = FloatArray(dropCount)
    val dropSpeed = FloatArray(dropCount)

    /** Near drops are longer, thicker, brighter and faster, and only they splash. */
    val dropNear = BooleanArray(dropCount)

    /** Splash ripples; a slot is free when its alpha is zero or less. */
    val splashX = FloatArray(SPLASH_CAPACITY)
    val splashY = FloatArray(SPLASH_CAPACITY)
    val splashR = FloatArray(SPLASH_CAPACITY)
    val splashAlpha = FloatArray(SPLASH_CAPACITY)

    /** Main bolt from the top of the screen, as a polyline of [boltSize] points. */
    private val boltCapacity = max(2, ceil(BOLT_LIMIT * height / (10f * unit)).toInt() + 2)
    val boltX = FloatArray(boltCapacity)
    val boltY = FloatArray(boltCapacity)
    var boltSize = 0
        private set

    /** Branch polylines, [BRANCH_POINTS] slots each; branch b has [branchSize]`[b]` points. */
    val branchX = FloatArray(boltCapacity * BRANCH_POINTS)
    val branchY = FloatArray(boltCapacity * BRANCH_POINTS)
    val branchSize = IntArray(boltCapacity)
    var branchCount = 0
        private set

    /** How many bolts have struck; the renderer rebuilds its paths when this changes. */
    var strikeCount = 0
        private set

    /** Bolt opacity for this frame, flickering while the bolt is fresh; 0 when none shows. */
    var boltAlpha = 0f
        private set

    /** Screen flash strength for this frame, 0–1. */
    var flash = 0f
        private set

    val activeSplashes: Int
        get() = splashAlpha.count { it > 0f }

    private var boltLife = 0f
    private var flashLevel = 0f
    private var untilStrikeMs = 1500f + random.nextFloat() * 2500f

    init {
        for (i in 0 until dropCount) {
            val near = random.nextFloat() < 0.3f
            dropNear[i] = near
            dropX[i] = random.nextFloat() * width * 1.2f - width * 0.1f
            dropY[i] = random.nextFloat() * height
            dropLen[i] = unit * if (near) 34f + random.nextFloat() * 26f else 14f + random.nextFloat() * 14f
            dropSpeed[i] = unit * if (near) 26f + random.nextFloat() * 10f else 14f + random.nextFloat() * 8f
        }
    }

    /** Advances the simulation by [dtMs] milliseconds. Does nothing when not [animated]. */
    fun tick(dtMs: Float) {
        if (!animated || dtMs <= 0f) return
        val clampedMs = min(MAX_STEP_MS, dtMs)
        val dt = clampedMs / FRAME_MS

        for (s in 0 until SPLASH_CAPACITY) {
            if (splashAlpha[s] <= 0f) continue
            splashR[s] += 1.2f * unit * dt
            splashAlpha[s] -= 0.03f * dt
        }

        for (i in 0 until dropCount) {
            val v = dropSpeed[i]
            dropY[i] += v * dt
            dropX[i] += v * wind * dt
            if (dropY[i] > height) {
                if (dropNear[i] && random.nextFloat() < 0.6f) {
                    addSplash(dropX[i], height - random.nextFloat() * height * SPLASH_BAND)
                }
                // Respawn just above the top. The mockup adds up to 60 px more stagger; keeping it
                // within one drop length holds every drop inside [-len, height].
                dropY[i] = -dropLen[i] * random.nextFloat()
                dropX[i] = random.nextFloat() * width * 1.2f - width * 0.15f
            }
        }

        if (storm) stepLightning(clampedMs, dt)
    }

    private fun stepLightning(clampedMs: Float, dt: Float) {
        untilStrikeMs -= clampedMs
        if (untilStrikeMs <= 0f) {
            makeBolt()
            strikeCount++
            boltLife = 1f
            flashLevel = 1f
            untilStrikeMs = 3500f + random.nextFloat() * 4000f
        }
        if (boltLife > 0f) {
            boltAlpha = if (boltLife > 0.6f) (if (random.nextFloat() < 0.5f) 1f else 0.35f) else boltLife
            boltLife -= 0.025f * dt
        } else {
            boltAlpha = 0f
        }
        if (flashLevel > 0f) {
            flash = flashLevel
            flashLevel -= 0.06f * dt
        } else {
            flash = 0f
        }
    }

    private fun addSplash(x: Float, y: Float) {
        for (s in 0 until SPLASH_CAPACITY) {
            if (splashAlpha[s] <= 0f) {
                splashX[s] = x
                splashY[s] = y
                splashR[s] = 2f * unit
                splashAlpha[s] = 0.6f
                return
            }
        }
        // Pool full: skip this splash rather than allocate.
    }

    /** A jagged bolt from the top to 35–60% of the height, with short branches off it. */
    private fun makeBolt() {
        val limit = BOLT_LIMIT * height
        var x = width * (0.2f + random.nextFloat() * 0.6f)
        var y = 0f
        val end = min(limit, height * (0.35f + random.nextFloat() * 0.25f))
        boltSize = 0
        boltX[boltSize] = x
        boltY[boltSize] = y
        boltSize++
        while (y < end && boltSize < boltCapacity) {
            y = min(end, y + (10f + random.nextFloat() * 22f) * unit)
            x += (random.nextFloat() - 0.5f) * 40f * unit
            boltX[boltSize] = x
            boltY[boltSize] = y
            boltSize++
        }

        branchCount = 0
        for (i in 2 until boltSize - 1) {
            if (random.nextFloat() >= 0.3f) continue
            val base = branchCount * BRANCH_POINTS
            var bx = boltX[i]
            var by = boltY[i]
            val dir = if (random.nextFloat() < 0.5f) -1f else 1f
            branchX[base] = bx
            branchY[base] = by
            var n = 1
            for (j in 0 until BRANCH_POINTS - 1) {
                val ny = by + (8f + random.nextFloat() * 14f) * unit
                // Branches stop rather than reach below the upper 60% of the screen.
                if (ny > limit) break
                by = ny
                bx += dir * (6f + random.nextFloat() * 16f) * unit
                branchX[base + n] = bx
                branchY[base + n] = by
                n++
            }
            if (n >= 2) {
                branchSize[branchCount] = n
                branchCount++
            }
        }
    }

    companion object {
        /** One mockup animation frame, in milliseconds. */
        const val FRAME_MS = 16.7f

        /** Longest step taken at once, as in the mockup, so a stalled frame does not jump. */
        const val MAX_STEP_MS = 50f

        const val SPLASH_CAPACITY = 128
        const val BRANCH_POINTS = 5

        /** Splashes land within this fraction of the height above the bottom edge. */
        const val SPLASH_BAND = 0.06f

        /** Bolts stay within this fraction of the height from the top. */
        const val BOLT_LIMIT = 0.6f

        /** The mockup canvas (288 × 520 CSS px at 2×) that the base drop counts were tuned for. */
        private const val REF_AREA = 576f * 1040f

        /**
         * 190 drops for rain and 300 for a storm on the mockup canvas, scaled with the screen area
         * (within 0.6–2.5×). A still frame keeps a quarter of them.
         */
        fun dropCountFor(width: Float, height: Float, unit: Float, storm: Boolean, animated: Boolean): Int {
            if (width <= 0f || height <= 0f || unit <= 0f) return 0
            val area = (width / unit) * (height / unit)
            val scale = (area / REF_AREA).coerceIn(0.6f, 2.5f)
            val base = (if (storm) 300 else 190) * scale
            return (if (animated) base else base / 4f).toInt()
        }
    }
}
