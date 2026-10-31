package com.iwankurnia.weather.ui.background

import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * The falling-rain background from `mockups/rain-storm.html`: two bands of heavy cloud rolling
 * across the top, then the [RainModel]'s drops, splashes, lightning and flash on top of them.
 * Reading [clock] while drawing redraws every frame; the caller ticks [model] in its frame loop.
 */
@Composable
internal fun RainLayer(model: RainModel?, isDay: Boolean, clock: LongState) {
    CloudBand(clock, isDay, topDp = -60f, alpha = 0.9f, halfPeriodMs = 22_000L, reverse = false)
    CloudBand(clock, isDay, topDp = -20f, alpha = 0.55f, halfPeriodMs = 30_000L, reverse = true)
    if (model != null) RainDrops(model, isDay, clock)
}

/** Blob centres (fractions of the band), radii in dp, and colour, from `.band`. */
private class Blob(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val colour: Color)

private val BandBlobs = listOf(
    Blob(0.15f, 0.60f, 60f, 50f, Color(0xE628303C)),
    Blob(0.40f, 0.50f, 90f, 60f, Color(0xE637404E)),
    Blob(0.65f, 0.65f, 80f, 55f, Color(0xE62D3440)),
    Blob(0.88f, 0.50f, 100f, 60f, Color(0xE63C4452)),
)

/** CSS `brightness(.45)`: darkens the band at night. */
private fun Color.dimmed(factor: Float) = Color(red * factor, green * factor, blue * factor, alpha)

/**
 * One `.band`: 160% of the screen wide, 210 dp tall, rolling ±8% of its width with CSS
 * ease-in-out. [reverse] starts at the far end (`alternate-reverse`). The band is blurred once
 * (`blur(12px)`, 14 px at night) and then only rolled.
 */
@Composable
private fun CloudBand(
    clock: LongState,
    isDay: Boolean,
    topDp: Float,
    alpha: Float,
    halfPeriodMs: Long,
    reverse: Boolean,
) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val bandWidth = 1.6f * size.width
            val bandLeft = -0.3f * size.width
            val bandTop = topDp.dp.toPx()
            val bandHeight = 210.dp.toPx()
            val roll = 0.08f * bandWidth
            val radii = FloatArray(BandBlobs.size) { BandBlobs[it].rx.dp.toPx() }
            val band = blurredSprite(bandWidth, bandHeight, (if (isDay) 12.dp else 14.dp).toPx()) {
                // A CSS background stays inside its box.
                clipRect(0f, 0f, bandWidth, bandHeight) {
                    for (i in BandBlobs.indices) {
                        val blob = BandBlobs[i]
                        val c = if (isDay) blob.colour else blob.colour.dimmed(0.45f)
                        val brush = Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), Offset.Zero, radii[i])
                        withTransform({
                            translate(blob.cx * bandWidth, blob.cy * bandHeight)
                            scale(1f, blob.ry / blob.rx, Offset.Zero)
                        }) {
                            drawCircle(brush, radii[i], Offset.Zero)
                        }
                    }
                }
            }
            onDrawBehind {
                val p = pingPong(clock.longValue, halfPeriodMs, CssEaseInOut)
                val dx = roll * if (reverse) 1f - 2f * p else -1f + 2f * p
                drawSprite(band, Offset(bandLeft + dx, bandTop), alpha)
            }
        },
    )
}

/** Paths, strokes and paints reused across frames so drawing allocates nothing. */
private class RainPaint(density: Float, isDay: Boolean) {
    /** One mockup canvas pixel: the mockup draws at 2×, so half a dp. */
    private val unit = density * 0.5f

    val far = Path()
    val near = Path()
    val farStroke = Stroke(width = 1.2f * unit, cap = StrokeCap.Round)
    val nearStroke = Stroke(width = 2.2f * unit, cap = StrokeCap.Round)
    val splashStroke = Stroke(width = 1.5f * unit)

    // Drop colours with the layer alpha baked in; night drops are bluer and 20% fainter.
    val farColour = if (isDay) Color(225, 235, 248).copy(alpha = 0.28f) else Color(170, 190, 220).copy(alpha = 0.28f * 0.8f)
    val nearColour = if (isDay) Color(225, 235, 248).copy(alpha = 0.5f) else Color(170, 190, 220).copy(alpha = 0.5f * 0.8f)
    val splashColour = Color(230, 238, 250)
    val flashColour = if (isDay) Color.White else Color(200, 210, 255)
    val flashStrength = if (isDay) 0.28f else 0.35f

    val boltPath = NativePath()
    val branchPath = NativePath()
    var builtStrike = -1
    private val boltWidth = 5f * unit
    private val branchWidth = 2.5f * unit
    val boltPaint = strokePaint(android.graphics.Color.WHITE)

    // `shadowBlur: 40` on the 2× canvas: a cool glow, bluer at night. `setShadowLayer` is ignored
    // for paths on a hardware canvas, so the glow is three wider, fainter strokes under the core.
    private val glowColour = (if (isDay) Color(0xFFE6ECFF) else Color(0xFFB9C8FF)).toArgb()
    private val glowPaint = strokePaint(glowColour).apply { strokeCap = NativePaint.Cap.ROUND }
    private val glowWidths = floatArrayOf(36f * unit, 22f * unit, 12f * unit)
    private val glowAlphas = floatArrayOf(0.07f, 0.12f, 0.22f)

    private fun strokePaint(colour: Int) = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.STROKE
        strokeJoin = NativePaint.Join.ROUND
        color = colour
    }

    fun rebuildBolt(model: RainModel) {
        boltPath.rewind()
        if (model.boltSize > 0) {
            boltPath.moveTo(model.boltX[0], model.boltY[0])
            for (i in 1 until model.boltSize) boltPath.lineTo(model.boltX[i], model.boltY[i])
        }
        branchPath.rewind()
        for (b in 0 until model.branchCount) {
            val base = b * RainModel.BRANCH_POINTS
            branchPath.moveTo(model.branchX[base], model.branchY[base])
            for (j in 1 until model.branchSize[b]) branchPath.lineTo(model.branchX[base + j], model.branchY[base + j])
        }
        builtStrike = model.strikeCount
    }

    fun drawBolt(scope: DrawScope, alpha: Float) {
        scope.drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            for (i in glowWidths.indices) {
                glowPaint.alpha = (glowAlphas[i] * alpha * 255f).toInt().coerceIn(0, 255)
                // Branches are half the bolt's width, so their glow is too.
                glowPaint.strokeWidth = glowWidths[i]
                native.drawPath(boltPath, glowPaint)
                glowPaint.strokeWidth = glowWidths[i] * 0.5f
                native.drawPath(branchPath, glowPaint)
            }
            boltPaint.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
            boltPaint.strokeWidth = boltWidth
            native.drawPath(boltPath, boltPaint)
            boltPaint.strokeWidth = branchWidth
            native.drawPath(branchPath, boltPaint)
        }
    }
}

@Composable
private fun RainDrops(model: RainModel, isDay: Boolean, clock: LongState) {
    val density = LocalDensity.current.density
    val paint = remember(density, isDay) { RainPaint(density, isDay) }
    Spacer(
        Modifier.fillMaxSize().drawBehind {
            clock.longValue // Redraw on every frame of the shared clock.
            val wind = model.wind
            paint.far.rewind()
            paint.near.rewind()
            for (i in 0 until model.dropCount) {
                val x = model.dropX[i]
                val y = model.dropY[i]
                val len = model.dropLen[i]
                val path = if (model.dropNear[i]) paint.near else paint.far
                path.moveTo(x, y)
                path.lineTo(x - len * wind, y - len)
            }
            drawPath(paint.far, paint.farColour, style = paint.farStroke)
            drawPath(paint.near, paint.nearColour, style = paint.nearStroke)

            for (s in 0 until RainModel.SPLASH_CAPACITY) {
                val a = model.splashAlpha[s]
                if (a <= 0f) continue
                val r = model.splashR[s]
                drawOval(
                    color = paint.splashColour,
                    topLeft = Offset(model.splashX[s] - r, model.splashY[s] - 0.3f * r),
                    size = Size(2f * r, 0.6f * r),
                    alpha = a.coerceAtMost(1f),
                    style = paint.splashStroke,
                )
            }

            if (model.boltAlpha > 0f) {
                if (paint.builtStrike != model.strikeCount) paint.rebuildBolt(model)
                paint.drawBolt(this, model.boltAlpha)
            }
            if (model.flash > 0f) {
                drawRect(paint.flashColour, alpha = (model.flash * paint.flashStrength).coerceIn(0f, 1f))
            }
        },
    )
}
