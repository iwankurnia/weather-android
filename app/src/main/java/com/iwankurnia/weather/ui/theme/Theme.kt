package com.iwankurnia.weather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * App theme: the phone's own (wallpaper) colours, light or dark, with Roboto Flex.
 * The weather screen itself takes its card colours from `cardTint`; this scheme is for
 * the plain screens (welcome, location refused, service error).
 */
@Composable
fun WeatherTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colours = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colours, typography = WeatherTypography) { ResponsiveText(content) }
}

/**
 * A circle whose edge waves in and out [lobes] times, [depth] being how far the valleys
 * dip as a fraction of the radius. Stands in for the Material 3 Expressive `MaterialShapes`,
 * which the resolved material3 (1.4.0) does not expose. The proportions match the
 * clip paths in the mockups.
 */
class LobedShape(private val lobes: Int, private val depth: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val points = 240
        for (i in 0 until points) {
            val angle = 2.0 * PI * i / points
            val radius = 1.0 - depth * (1.0 - cos(lobes * angle)) / 2.0
            val x = (cx + cx * radius * cos(angle)).toFloat()
            val y = (cy + cy * radius * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** The expressive badge shapes used on the main screen. */
object WeatherShapes {
    /** Scalloped nine-sided cookie, for "feels like" and the empty-state badges. */
    val Cookie9Sided: Shape = LobedShape(lobes = 9, depth = 0.14f)

    /** Twelve soft rays, for the UV index. */
    val Sunny: Shape = LobedShape(lobes = 12, depth = 0.12f)

    /** Four rounded leaves, for air quality. */
    val Clover4Leaf: Shape = LobedShape(lobes = 4, depth = 0.24f)
}
