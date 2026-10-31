package com.iwankurnia.weather.ui.background

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * Soft layers (clouds, the storm's cloud band, fog) are blurred once into a small bitmap and then
 * only moved. Blurring a full-screen layer with `Modifier.blur` on every frame kept the GPU busy
 * the whole time the background was on screen.
 */

/** Blur, in bitmap pixels, that a sprite is shrunk to before blurring; upscaling stays smooth. */
private const val SPRITE_SIGMA = 3f

/** A shape blurred once; [drawSprite] places it with the shape's own top-left at a point. */
internal class BlurredSprite(
    private val image: ImageBitmap,
    /** Blur margin around the shape, in screen pixels. */
    private val pad: Float,
    /** The bitmap's size on screen, margin included. */
    private val drawSize: IntSize,
) {
    fun draw(scope: DrawScope, topLeft: Offset, alpha: Float) {
        scope.translate(topLeft.x - pad, topLeft.y - pad) {
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstSize = drawSize,
                alpha = alpha,
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

internal fun DrawScope.drawSprite(sprite: BlurredSprite, topLeft: Offset, alpha: Float = 1f) =
    sprite.draw(this, topLeft, alpha)

/**
 * Draws [draw] (a shape within [width] × [height] pixels) into a bitmap and blurs it with a
 * Gaussian of standard deviation [sigma] pixels, which is what a CSS `blur()` length means. The
 * bitmap is shrunk so the blur is about [SPRITE_SIGMA] of its pixels: small and quick to blur,
 * and smooth when drawn back at full size.
 */
internal fun CacheDrawScope.blurredSprite(
    width: Float,
    height: Float,
    sigma: Float,
    draw: DrawScope.() -> Unit,
): BlurredSprite {
    val pad = ceil(3f * sigma)
    val factor = max(1f, sigma / SPRITE_SIGMA)
    val w = max(1, ceil((width + 2f * pad) / factor).toInt())
    val h = max(1, ceil((height + 2f * pad) / factor).toInt())
    val image = ImageBitmap(w, h)
    CanvasDrawScope().draw(this, layoutDirection, Canvas(image), Size(w.toFloat(), h.toFloat())) {
        withTransform({
            scale(1f / factor, 1f / factor, Offset.Zero)
            translate(pad, pad)
        }) { draw() }
    }
    val pixels = IntArray(w * h)
    image.readPixels(pixels)
    blurArgb(pixels, w, h, sigma / factor)
    image.asAndroidBitmap().setPixels(pixels, 0, w, 0, 0, w, h)
    image.prepareToDraw()
    return BlurredSprite(image, pad, IntSize(ceil(w * factor).toInt(), ceil(h * factor).toInt()))
}

/**
 * Widths of [n] box blurs that together approximate a Gaussian of standard deviation [sigma]
 * (each pass is a box; three are close to Gaussian).
 */
internal fun gaussianBoxSizes(sigma: Float, n: Int = 3): IntArray {
    val ideal = sqrt(12f * sigma * sigma / n + 1f)
    var lower = floor(ideal).toInt()
    if (lower % 2 == 0) lower--
    val upper = lower + 2
    val lowerCount = ((12f * sigma * sigma - n * lower * lower - 4f * n * lower - 3f * n) / (-4f * lower - 4f)).roundToInt()
    return IntArray(n) { if (it < lowerCount) lower else upper }
}

/**
 * Blurs non-premultiplied ARGB [pixels] in place, treating everything outside as transparent.
 * Works on premultiplied channels so colours don't darken where they fade out.
 */
internal fun blurArgb(pixels: IntArray, width: Int, height: Int, sigma: Float) {
    if (sigma <= 0f || width <= 0 || height <= 0) return
    val count = width * height
    val a = FloatArray(count)
    val r = FloatArray(count)
    val g = FloatArray(count)
    val b = FloatArray(count)
    for (i in 0 until count) {
        val c = pixels[i]
        val alpha = (c ushr 24).toFloat()
        val k = alpha / 255f
        a[i] = alpha
        r[i] = ((c shr 16) and 0xFF) * k
        g[i] = ((c shr 8) and 0xFF) * k
        b[i] = (c and 0xFF) * k
    }
    val scratch = FloatArray(count)
    val radii = gaussianBoxSizes(sigma).map { (it - 1) / 2 }.filter { it > 0 }
    for (channel in arrayOf(a, r, g, b)) {
        for (radius in radii) {
            boxBlurRows(channel, scratch, width, height, radius)
            boxBlurColumns(scratch, channel, width, height, radius)
        }
    }
    for (i in 0 until count) {
        val alpha = a[i].coerceIn(0f, 255f)
        if (alpha < 0.5f) {
            pixels[i] = 0
            continue
        }
        val k = 255f / alpha
        pixels[i] = (alpha.roundToInt() shl 24) or
            ((r[i] * k).roundToInt().coerceIn(0, 255) shl 16) or
            ((g[i] * k).roundToInt().coerceIn(0, 255) shl 8) or
            (b[i] * k).roundToInt().coerceIn(0, 255)
    }
}

/** Running-sum box blur of width 2 × [radius] + 1 along each row. */
private fun boxBlurRows(src: FloatArray, dst: FloatArray, width: Int, height: Int, radius: Int) {
    val scale = 1f / (2 * radius + 1)
    for (y in 0 until height) {
        val row = y * width
        var sum = 0f
        for (x in 0..minOf(radius, width - 1)) sum += src[row + x]
        for (x in 0 until width) {
            dst[row + x] = sum * scale
            val add = x + radius + 1
            if (add < width) sum += src[row + add]
            val remove = x - radius
            if (remove >= 0) sum -= src[row + remove]
        }
    }
}

/** As [boxBlurRows], down each column. */
private fun boxBlurColumns(src: FloatArray, dst: FloatArray, width: Int, height: Int, radius: Int) {
    val scale = 1f / (2 * radius + 1)
    for (x in 0 until width) {
        var sum = 0f
        for (y in 0..minOf(radius, height - 1)) sum += src[y * width + x]
        for (y in 0 until height) {
            dst[y * width + x] = sum * scale
            val add = y + radius + 1
            if (add < height) sum += src[add * width + x]
            val remove = y - radius
            if (remove >= 0) sum -= src[remove * width + x]
        }
    }
}
