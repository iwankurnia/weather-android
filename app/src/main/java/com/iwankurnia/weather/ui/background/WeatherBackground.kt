package com.iwankurnia.weather.ui.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iwankurnia.weather.model.Condition

/**
 * The full-screen sky behind the main screen: a gradient for [condition] by day or night, with a
 * gentle moving layer on top. Temperature never changes it. With "Remove animations" on it shows
 * one still frame and runs no frame loop; storms then show no lightning.
 */
@Composable
fun WeatherBackground(condition: Condition, isDay: Boolean, modifier: Modifier = Modifier) {
    val animated = rememberAnimationsEnabled()
    val sky = remember(condition, isDay) { skyGradient(condition, isDay).brush() }
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    val rainy = condition == Condition.RAIN || condition == Condition.THUNDERSTORM
    val rain = remember(size, condition, animated, density) {
        if (rainy && size.width > 0 && size.height > 0) {
            RainModel(
                width = size.width.toFloat(),
                height = size.height.toFloat(),
                storm = condition == Condition.THUNDERSTORM,
                animated = animated,
                unit = density * 0.5f,
            )
        } else {
            null
        }
    }

    // Milliseconds since the loop started; every layer reads it only while drawing.
    val clock = remember { mutableLongStateOf(0L) }
    val currentRain by rememberUpdatedState(rain)
    if (animated) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            var last = start
            while (true) {
                val now = withFrameNanos { it }
                currentRain?.tick((now - last) / 1_000_000f)
                last = now
                clock.longValue = (now - start) / 1_000_000L
            }
        }
    }

    Box(
        modifier
            .clipToBounds()
            .background(sky)
            .onSizeChanged { size = it },
    ) {
        when (condition) {
            Condition.CLEAR -> if (isDay) {
                SunGlow(clock)
            } else {
                StarField(clock)
                Moon()
            }
            Condition.PARTLY_CLOUDY -> if (isDay) {
                SunGlow(clock, opacity = 0.7f)
                Clouds(clock, PartlyCloudyDayClouds, CloudWhite)
            } else {
                StarField(clock, opacity = 0.6f)
                Moon()
                Clouds(clock, PartlyCloudyNightClouds, CloudNight)
            }
            Condition.CLOUDY -> Clouds(clock, CloudyClouds, if (isDay) CloudGrey else CloudNight)
            Condition.HAZE -> Fog(clock, if (isDay) FogDay else FogNight)
            Condition.RAIN, Condition.THUNDERSTORM -> RainLayer(rain, isDay, clock)
        }
    }
}

/** A background with a hero line and one card in [cardTint] colours, to judge contrast. */
@Composable
private fun BackgroundPreview(condition: Condition, isDay: Boolean) {
    val tint = cardTint(condition, isDay)
    Box(Modifier.fillMaxSize()) {
        WeatherBackground(condition, isDay, Modifier.fillMaxSize())
        Column(Modifier.padding(16.dp)) {
            Text("34°", color = tint.onSky, fontSize = 96.sp, fontWeight = FontWeight.Light)
            Text(
                "${condition.name.lowercase().replace('_', ' ')} · ${if (isDay) "day" else "night"}",
                color = tint.onSky,
                fontSize = 18.sp,
            )
            Column(
                Modifier
                    .padding(top = 140.dp)
                    .fillMaxWidth()
                    .background(tint.surface, RoundedCornerShape(28.dp))
                    .padding(16.dp),
            ) {
                Text("Rain likely from 15:00", color = tint.onSurface, fontWeight = FontWeight.Medium)
                Text("High 34° · Low 25°", color = tint.subtle, fontSize = 13.sp)
                Text(
                    "Now",
                    color = tint.onPrimary,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .background(tint.primary, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                Text(
                    "15:00",
                    color = tint.onContainer,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(tint.container, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private const val PREVIEW_W = 360
private const val PREVIEW_H = 760

@Preview(name = "Clear, day", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun ClearDayPreview() = BackgroundPreview(Condition.CLEAR, isDay = true)

@Preview(name = "Clear, night", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun ClearNightPreview() = BackgroundPreview(Condition.CLEAR, isDay = false)

@Preview(name = "Partly cloudy, day", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PartlyCloudyDayPreview() = BackgroundPreview(Condition.PARTLY_CLOUDY, isDay = true)

@Preview(name = "Partly cloudy, night", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun PartlyCloudyNightPreview() = BackgroundPreview(Condition.PARTLY_CLOUDY, isDay = false)

@Preview(name = "Cloudy, day", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun CloudyDayPreview() = BackgroundPreview(Condition.CLOUDY, isDay = true)

@Preview(name = "Cloudy, night", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun CloudyNightPreview() = BackgroundPreview(Condition.CLOUDY, isDay = false)

@Preview(name = "Rain, day", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun RainDayPreview() = BackgroundPreview(Condition.RAIN, isDay = true)

@Preview(name = "Rain, night", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun RainNightPreview() = BackgroundPreview(Condition.RAIN, isDay = false)

@Preview(name = "Thunderstorm, day", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun ThunderstormDayPreview() = BackgroundPreview(Condition.THUNDERSTORM, isDay = true)

@Preview(name = "Thunderstorm, night", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun ThunderstormNightPreview() = BackgroundPreview(Condition.THUNDERSTORM, isDay = false)

@Preview(name = "Haze, day", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun HazeDayPreview() = BackgroundPreview(Condition.HAZE, isDay = true)

@Preview(name = "Haze, night", widthDp = PREVIEW_W, heightDp = PREVIEW_H)
@Composable
private fun HazeNightPreview() = BackgroundPreview(Condition.HAZE, isDay = false)
