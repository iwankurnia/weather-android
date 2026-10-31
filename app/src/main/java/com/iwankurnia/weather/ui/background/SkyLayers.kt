package com.iwankurnia.weather.ui.background

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/*
 * Sky layers from `mockups/backgrounds-day-night.html`: sun glow, moon and stars, drifting clouds
 * and fog. Sizes are the mockup's CSS pixels read as dp. Horizontal cloud positions are scaled from
 * the 248 px wide mockup screen to the real width. Every layer reads the shared [LongState] clock
 * (milliseconds) only while drawing, so animation redraws without recomposing; a clock stuck at 0
 * gives the reduced-motion still frame. Blurred layers are blurred once ([blurredSprite]) and only
 * moved after that.
 */

/** CSS `ease-in-out`. */
internal val CssEaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** Width of the mockup's small phone screen, used to place clouds proportionally. */
private const val MOCKUP_WIDTH_DP = 248f

/**
 * Progress 0–1–0 of a CSS `alternate` animation: 0 at t=0, 1 after [halfPeriodMs], back to 0
 * after twice that, eased on each leg.
 */
internal fun pingPong(timeMs: Long, halfPeriodMs: Long, easing: Easing): Float {
    val cycle = timeMs % (2 * halfPeriodMs)
    val leg = if (cycle < halfPeriodMs) cycle else 2 * halfPeriodMs - cycle
    return easing.transform(leg.toFloat() / halfPeriodMs)
}

/** The sun glowing off the top-right corner, pulsing every 4 s (`.sun`). */
@Composable
internal fun SunGlow(clock: LongState, opacity: Float = 1f) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val radius = 110.dp.toPx()
            val centre = Offset(size.width - 40.dp.toPx(), 40.dp.toPx())
            // `radial-gradient(circle, …)` defaults to farthest-corner: √2 × the radius of the box.
            val brush = Brush.radialGradient(
                0f to Color(0xFFFFFBE0),
                0.3f to Color(0xCCFFEC96),
                0.7f to Color(0x00FFDC78),
                center = centre,
                radius = radius * sqrt(2f),
            )
            onDrawBehind {
                val p = pingPong(clock.longValue, 2_000L, CssEaseInOut)
                val scale = 1f + 0.1f * p
                withTransform({ scale(scale, scale, centre) }) {
                    drawCircle(brush, radius, centre, alpha = opacity * (1f - 0.2f * p))
                }
            }
        },
    )
}

/** Star positions (fractions of the screen) and radii in dp (`.stars`). */
private val Stars = floatArrayOf(
    0.15f, 0.06f, 1.4f,
    0.68f, 0.04f, 1f,
    0.82f, 0.18f, 1.4f,
    0.38f, 0.12f, 1f,
    0.55f, 0.24f, 1f,
    0.10f, 0.28f, 1.4f,
    0.90f, 0.32f, 1f,
    0.28f, 0.36f, 1f,
)

/** Small soft stars that twinkle every 3 s. */
@Composable
internal fun StarField(clock: LongState, opacity: Float = 1f) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val count = Stars.size / 3
            val centres = Array(count) { Offset(Stars[it * 3] * size.width, Stars[it * 3 + 1] * size.height) }
            val radii = FloatArray(count) { Stars[it * 3 + 2].dp.toPx() }
            val brushes = Array(count) {
                Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), centres[it], radii[it])
            }
            onDrawBehind {
                val alpha = opacity * (1f - 0.4f * pingPong(clock.longValue, 1_500L, CssEaseInOut))
                for (i in 0 until count) drawCircle(brushes[i], radii[i], centres[i], alpha = alpha)
            }
        },
    )
}

/** A pale crescent near the top right (`.moon`: a box shadow peeking out from its own circle). */
@Composable
internal fun Moon() {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val r = 20.dp.toPx()
            val body = Offset(size.width - 30.dp.toPx() - r, 60.dp.toPx() + r)
            val lit = Offset(body.x - 10.dp.toPx(), body.y + 7.dp.toPx())
            val cutOut = Path().apply { addOval(Rect(body, r)) }
            onDrawBehind {
                clipPath(cutOut, ClipOp.Difference) {
                    drawCircle(Color(0xFFF4F1D0), r, lit)
                }
            }
        },
    )
}

/** One drifting cloud: left and top in mockup px, width in dp, drift half-period in ms. */
internal class CloudSpec(val left: Float, val top: Float, val width: Float, val durationMs: Long = 18_000L)

internal val PartlyCloudyDayClouds = listOf(CloudSpec(200f, 300f, 110f), CloudSpec(210f, 80f, 80f, 22_000L))
internal val PartlyCloudyNightClouds = listOf(CloudSpec(190f, 300f, 110f), CloudSpec(200f, 90f, 80f, 22_000L))
internal val CloudyClouds = listOf(
    CloudSpec(170f, 70f, 130f),
    CloudSpec(-20f, 110f, 100f, 20_000L),
    CloudSpec(180f, 300f, 120f, 26_000L),
)

/** Cloud fills: `.cl`, `.cl.g` (cloudy day) and `.cl.n` (night). */
internal val CloudWhite = Color(0xB3FFFFFF)
internal val CloudGrey = Color(0x8CE1E6EE)
internal val CloudNight = Color(0x4D96A0B9)

/**
 * Soft pill-shaped clouds with a puff on top, drifting from −25 to +30 dp and back (`.cl`).
 * Each cloud is blurred once (`blur(2px)`) and then only moved.
 */
@Composable
internal fun Clouds(clock: LongState, clouds: List<CloudSpec>, colour: Color) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val xScale = size.width / MOCKUP_WIDTH_DP.dp.toPx()
            val height = 42.dp.toPx()
            val corner = CornerRadius(height / 2f)
            val from = (-25).dp.toPx()
            val span = 55.dp.toPx()
            val puffRise = 0.6f * height
            val sprites = Array(clouds.size) { i ->
                val w = clouds[i].width.dp.toPx()
                blurredSprite(w, height + puffRise, 2.dp.toPx()) {
                    drawRoundRect(colour, Offset(0f, puffRise), Size(w, height), corner)
                    // `::before`: 55% wide, 140% tall, 18% in from the left, 60% above the top.
                    drawOval(colour, Offset(0.18f * w, 0f), Size(0.55f * w, 1.4f * height))
                }
            }
            onDrawBehind {
                val t = clock.longValue
                for (i in clouds.indices) {
                    val cloud = clouds[i]
                    val dx = from + span * pingPong(t, cloud.durationMs, LinearEasing)
                    val x = cloud.left.dp.toPx() * xScale + dx
                    drawSprite(sprites[i], Offset(x, cloud.top.dp.toPx() - puffRise))
                }
            }
        },
    )
}

/** Fog fills: `.fog.d` and `.fog.n`. */
internal val FogDay = Color(0x73FFFFFF)
internal val FogNight = Color(0x2EC8BEAA)

/** Two wide, heavily blurred bands of mist easing side to side (`.fog`), blurred once. */
@Composable
internal fun Fog(clock: LongState, colour: Color) {
    Spacer(
        Modifier.fillMaxSize().drawWithCache {
            val left = -0.4f * size.width
            val bandSize = Size(1.8f * size.width, 70.dp.toPx())
            val tops = floatArrayOf(60.dp.toPx(), 260.dp.toPx())
            val from = (-25).dp.toPx()
            val span = 55.dp.toPx()
            val band = blurredSprite(bandSize.width, bandSize.height, 14.dp.toPx()) {
                drawOval(colour, Offset.Zero, bandSize)
            }
            onDrawBehind {
                val t = clock.longValue
                drawSprite(band, Offset(left + from + span * pingPong(t, 12_000L, CssEaseInOut), tops[0]))
                drawSprite(band, Offset(left + from + span * pingPong(t, 16_000L, CssEaseInOut), tops[1]))
            }
        },
    )
}
