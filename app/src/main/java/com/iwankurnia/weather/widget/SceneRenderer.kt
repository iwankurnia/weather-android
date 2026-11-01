package com.iwankurnia.weather.widget

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.ui.background.skyGradient
import kotlin.math.roundToInt
import kotlin.random.Random

/*
 * A still frame of the app's weather background, for the scene widget (widgets cannot animate).
 * Drawn in "design units": the widget is 150 units tall, as in `mockups/scene-widgets.html`, and
 * as wide as its shape needs. Rain and thunderstorm follow the falling-rain design: dark cloud
 * band, two layers of slanted rain, splashes along the bottom, and a glowing bolt in storms.
 */

private const val DESIGN_HEIGHT = 150f

/** Widest bitmap (px) the scene is drawn at; RemoteViews bitmaps must stay small. */
private const val MAX_BITMAP_WIDTH = 960

/** One rain streak, from (x, y) up-wind to (endX, endY), in design units. */
internal data class SceneDrop(
    val x: Float,
    val y: Float,
    val endX: Float,
    val endY: Float,
    val near: Boolean,
)

internal fun sceneBitmapSizeOrNull(widthDp: Float, heightDp: Float, density: Float): Pair<Int, Int>? {
    if (widthDp <= 0f || heightDp <= 0f) return null
    var w = widthDp * density
    var h = heightDp * density
    if (w > MAX_BITMAP_WIDTH) {
        h *= MAX_BITMAP_WIDTH / w
        w = MAX_BITMAP_WIDTH.toFloat()
    }
    return w.roundToInt() to h.roundToInt()
}

internal fun sceneBitmapSize(widthDp: Float, heightDp: Float, density: Float): Pair<Int, Int> =
    requireNotNull(sceneBitmapSizeOrNull(widthDp, heightDp, density))

/** Near drops are longer and lighter-weight far ones finer; storms have more and slant further. */
internal fun sceneRainDrops(seed: Int, width: Float, height: Float, storm: Boolean): List<SceneDrop> {
    val random = Random(seed)
    val wind = if (storm) 0.28f else 0.1f
    val far = if (storm) 70 else 45
    val near = if (storm) 30 else 18
    val scale = width / 310f // the mockup's drop counts are for a 310-unit-wide widget
    return List(((far + near) * scale).roundToInt()) { i ->
        val isNear = i >= (far * scale).roundToInt()
        val length = if (isNear) random.nextFloat() * 8f + 14f else random.nextFloat() * 5f + 7f
        val x = random.nextFloat() * (width + 40f) - 20f
        val y = length + random.nextFloat() * (height - length)
        SceneDrop(x, y, x - length * wind, y - length, isNear)
    }
}

/** The lightning bolt: a zigzag from the top centre to just past half way, and one short branch. */
internal fun sceneBolt(seed: Int, width: Float, height: Float): Pair<List<Pair<Float, Float>>, List<Pair<Float, Float>>> {
    val random = Random(seed * 31 + 7)
    // Starts in the gap between the two top labels, so it never crosses the text.
    var x = width * (0.42f + random.nextFloat() * 0.1f)
    var y = 0f
    val main = mutableListOf(x to y)
    while (y < height * 0.55f) {
        y += random.nextFloat() * 7f + 6f
        x += random.nextFloat() * 24f - 12f
        main += x to y
    }
    val (bx, by) = main[minOf(3, main.lastIndex)]
    val branch = List(4) { k -> bx + k * 7f to by + k * 8f }
    return main to branch
}

/** Draws the scene for [condition] at [widthPx]×[heightPx]; [alpha] fades it for the opacity setting. */
internal fun renderScene(
    condition: Condition,
    isDay: Boolean,
    widthPx: Int,
    heightPx: Int,
    seed: Int,
    alpha: Float,
): Bitmap {
    val bitmap = createBitmap(widthPx, heightPx)
    if (alpha <= 0f) return bitmap
    val canvas = Canvas(bitmap)
    val scale = heightPx / DESIGN_HEIGHT
    val w = widthPx / scale
    val h = DESIGN_HEIGHT
    canvas.saveLayerAlpha(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), (alpha.coerceIn(0f, 1f) * 255).roundToInt())
    canvas.scale(scale, scale)

    val sky = skyGradient(condition, isDay)
    canvas.drawRect(0f, 0f, w, h, Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(sky.top.toArgb(), sky.middle.toArgb(), sky.bottom.toArgb()),
            floatArrayOf(0f, sky.middleStop, 1f),
            Shader.TileMode.CLAMP,
        )
    })

    when (condition) {
        Condition.CLEAR -> if (isDay) sun(canvas, w, 1f) else { stars(canvas, w, h, 1f); moon(canvas, w) }
        Condition.PARTLY_CLOUDY -> {
            if (isDay) sun(canvas, w, 0.7f) else { stars(canvas, w, h, 0.6f); moon(canvas, w) }
            val colour = if (isDay) 0x9EFFFFFF.toInt() else 0x4D96A0B9
            cloud(canvas, w * 0.48f, 62f, 80f, colour)
            cloud(canvas, w * 0.13f, 104f, 60f, colour)
        }
        Condition.CLOUDY -> {
            val colour = if (isDay) 0x8CE1E6EE.toInt() else 0x4D96A0B9
            cloud(canvas, w * 0.52f, 50f, 110f, colour)
            cloud(canvas, w * 0.06f, 96f, 90f, colour)
            cloud(canvas, w * 0.65f, 110f, 100f, colour)
        }
        Condition.HAZE -> {
            val colour = if (isDay) 0x73FFFFFF else 0x2EC8BEAA
            fog(canvas, w, 40f, colour)
            fog(canvas, w, 100f, colour)
        }
        Condition.RAIN, Condition.THUNDERSTORM -> {
            val storm = condition == Condition.THUNDERSTORM
            cloudBand(canvas, w, isDay)
            rain(canvas, w, h, seed, storm, isDay)
            if (storm) bolt(canvas, w, h, seed, isDay)
        }
    }
    canvas.restore()
    return bitmap
}

private fun blurred(colour: Int, radius: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = colour
    maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
}

private fun sun(canvas: Canvas, w: Float, opacity: Float) {
    val cx = w - 30f
    val cy = 20f
    canvas.drawCircle(cx, cy, 80f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, cy, 80f,
            intArrayOf(0xFFFFFBE0.toInt(), 0xCCFFEC96.toInt(), 0x00FFDC78),
            floatArrayOf(0f, 0.3f, 0.7f),
            Shader.TileMode.CLAMP,
        )
        alpha = (opacity * 255).roundToInt()
    })
}

private fun stars(canvas: Canvas, w: Float, h: Float, opacity: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); alpha = (opacity * 255).roundToInt() }
    for ((fx, fy, r) in listOf(Triple(.12f, .18f, 1.3f), Triple(.45f, .10f, 1f), Triple(.70f, .30f, 1.3f), Triple(.30f, .60f, 1f), Triple(.88f, .66f, 1f))) {
        canvas.drawCircle(w * fx, h * fy, r, paint)
    }
}

/** The mockup's crescent: a disc offset down-left, minus the disc it hangs from. */
private fun moon(canvas: Canvas, w: Float) {
    val cx = w - 24f - 15f
    val cy = 14f + 15f
    val lit = Path().apply { addCircle(cx - 8f, cy + 6f, 15f, Path.Direction.CW) }
    val shade = Path().apply { addCircle(cx, cy, 15f, Path.Direction.CW) }
    lit.op(shade, Path.Op.DIFFERENCE)
    canvas.drawPath(lit, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F1D0.toInt() })
}

/** A soft cloud: a pill with a puff on top, blurred like the app's clouds. */
private fun cloud(canvas: Canvas, left: Float, top: Float, width: Float, colour: Int) {
    val paint = blurred(colour, 2f)
    canvas.drawRoundRect(RectF(left, top, left + width, top + 30f), 15f, 15f, paint)
    canvas.drawOval(RectF(left + width * 0.18f, top - 18f, left + width * 0.73f, top + 24f), paint)
}

private fun fog(canvas: Canvas, w: Float, top: Float, colour: Int) {
    canvas.drawOval(RectF(-0.3f * w, top, 1.3f * w, top + 46f), blurred(colour, 10f))
}

/** Heavy clouds across the top; dimmed at night, as in the app. */
private fun cloudBand(canvas: Canvas, w: Float, isDay: Boolean) {
    val dim = if (isDay) 1f else 0.45f
    fun shade(r: Int, g: Int, b: Int) = (0xE6 shl 24) or ((r * dim).roundToInt() shl 16) or ((g * dim).roundToInt() shl 8) or (b * dim).roundToInt()
    for ((fx, rx, ry, c) in listOf(
        Quad(.15f, 45f, 34f, shade(40, 48, 60)),
        Quad(.42f, 65f, 40f, shade(55, 64, 78)),
        Quad(.66f, 58f, 36f, shade(45, 52, 64)),
        Quad(.90f, 70f, 40f, shade(60, 68, 82)),
    )) {
        val cx = w * fx
        val cy = 22f
        canvas.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), blurred(c, 9f))
    }
}

private data class Quad(val a: Float, val b: Float, val c: Float, val d: Int)

private fun rain(canvas: Canvas, w: Float, h: Float, seed: Int, storm: Boolean, isDay: Boolean) {
    val rgb = if (isDay) 0xE1EBF8 else 0xAABEDC
    val dim = if (isDay) 1f else 0.8f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    for (drop in sceneRainDrops(seed, w, h, storm)) {
        val a = (if (drop.near) 0.5f else 0.28f) * dim
        paint.color = ((a * 255).roundToInt() shl 24) or rgb
        paint.strokeWidth = if (drop.near) 1.6f else 0.9f
        canvas.drawLine(drop.x, drop.y, drop.endX, drop.endY, paint)
    }
    val random = Random(seed + 1)
    val splash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x73E6EEFA }
    repeat(if (storm) 7 else 5) {
        val x = 10f + random.nextFloat() * (w - 20f)
        val y = h - 3f - random.nextFloat() * 6f
        val rx = 3f + random.nextFloat() * 3f
        canvas.drawOval(RectF(x - rx, y - 1.2f, x + rx, y + 1.2f), splash)
    }
}

private fun bolt(canvas: Canvas, w: Float, h: Float, seed: Int, isDay: Boolean) {
    val (main, branch) = sceneBolt(seed, w, h)
    fun path(points: List<Pair<Float, Float>>) = Path().apply {
        moveTo(points[0].first, points[0].second)
        points.drop(1).forEach { (x, y) -> lineTo(x, y) }
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    // Only the halo is blurred; the bolt itself stays crisp on top.
    canvas.drawPath(path(main), Paint(stroke).apply {
        color = if (isDay) 0x8CE6ECFF.toInt() else 0x8CB9C8FF.toInt()
        strokeWidth = 7f
        maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
    })
    canvas.drawPath(path(main), Paint(stroke).apply { color = 0xFFFFFFFF.toInt(); strokeWidth = 2.4f })
    canvas.drawPath(path(branch), Paint(stroke).apply { color = 0xFFFFFFFF.toInt(); strokeWidth = 1.3f })
    canvas.drawRect(0f, 0f, w, h, Paint().apply { color = if (isDay) 0x14FFFFFF else 0x14C8D2FF })
}
