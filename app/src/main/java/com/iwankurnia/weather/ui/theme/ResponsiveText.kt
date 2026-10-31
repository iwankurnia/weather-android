package com.iwankurnia.weather.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density

/** Window width (dp) of the phone the mockups' text sizes were drawn for. */
private const val REFERENCE_WIDTH_DP = 411f

/**
 * How much to scale text for a window [widthDp] wide: proportional to the reference phone,
 * kept between 85% (small phones, split screen) and 125% (tablets, landscape).
 */
internal fun textScaleFor(widthDp: Int): Float = (widthDp / REFERENCE_WIDTH_DP).coerceIn(0.85f, 1.25f)

/**
 * Scales every sp text size inside [content] to the window width. Works through the font
 * scale, so it multiplies with the phone's own font-size setting rather than replacing it.
 */
@Composable
fun ResponsiveText(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val widthDp = (LocalWindowInfo.current.containerSize.width / density.density).toInt()
    val scale = textScaleFor(widthDp)
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * scale),
        content = content,
    )
}
