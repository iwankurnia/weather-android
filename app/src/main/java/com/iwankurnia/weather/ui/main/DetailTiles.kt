package com.iwankurnia.weather.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Masks
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.background.CardColours
import com.iwankurnia.weather.ui.theme.WeatherShapes
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme
import java.time.Duration
import java.time.LocalTime
import kotlin.math.roundToInt

/** The "Details" section: six tiles, two per row. [nowTime] places the sun on its arc. */
@Composable
internal fun DetailTiles(
    snapshot: WeatherSnapshot,
    nowTime: LocalTime,
    colours: CardColours,
    modifier: Modifier = Modifier,
) {
    val details = snapshot.details
    Column(modifier.fillMaxWidth()) {
        SectionHeader(Icons.Rounded.Info, "Details", colours.onSky)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TileRow(
                { UvTile(details.uvIndex, snapshot.now.isDay, colours, it) },
                { AirTile(snapshot.air, colours, it) },
            )
            TileRow(
                { HumidityTile(snapshot.now.humidityPct, details.dewPointC, colours, it) },
                { WindTile(snapshot.now.windKmh, snapshot.now.windFromDeg, details.gustKmh, colours, it) },
            )
            TileRow(
                { RainfallTile(details.rainLast24hMm, details.rainTomorrowMm, colours, it) },
                { SunTile(details.sunrise, details.sunset, nowTime, colours, it) },
            )
        }
    }
}

@Composable
private fun TileRow(start: @Composable (Modifier) -> Unit, end: @Composable (Modifier) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        start(Modifier.weight(1f).fillMaxHeight())
        end(Modifier.weight(1f).fillMaxHeight())
    }
}

/** One rounded tile: [behind] draws under the content (the humidity fill). */
@Composable
private fun Tile(
    icon: ImageVector,
    label: String,
    colours: CardColours,
    modifier: Modifier = Modifier,
    tag: String? = null,
    behind: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .heightIn(min = 136.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colours.surface),
    ) {
        behind()
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(icon, contentDescription = null, tint = colours.subtle, modifier = Modifier.size(15.dp))
                Text(label, style = WeatherText.tileLabel, color = colours.subtle)
                if (tag != null) {
                    Text(
                        text = tag,
                        style = WeatherText.smallStrong,
                        color = colours.onContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(colours.container)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SmallText(text: String, colours: CardColours, modifier: Modifier = Modifier) {
    Text(text, style = WeatherText.tileSmall, color = colours.subtle, modifier = modifier.padding(top = 4.dp))
}

@Composable
private fun BigText(text: String, colours: CardColours, style: TextStyle = WeatherText.tileBig) {
    Text(text, style = style, color = colours.onSurface)
}

/** A number inside an expressive badge shape. */
@Composable
private fun ShapeBadge(text: String, shape: Shape, size: Dp, background: Color, content: Color, description: String) {
    Box(
        modifier = Modifier
            .size(size)
            .background(background, shape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = WeatherText.badge, color = content)
    }
}

@Composable
private fun UvTile(uv: Double?, isDay: Boolean, colours: CardColours, modifier: Modifier) {
    Tile(Icons.Rounded.LightMode, "UV index", colours, modifier) {
        val value = uv?.roundToInt()?.toString() ?: "—"
        ShapeBadge(value, WeatherShapes.Sunny, 64.dp, colours.primary, colours.onPrimary, "UV index $value")
        SmallText(uvAdvice(uv, isDay), colours)
    }
}

@Composable
private fun AirTile(air: AirQuality?, colours: CardColours, modifier: Modifier) {
    Tile(Icons.Rounded.Masks, "Air quality", colours, modifier, tag = if (air?.modelled == true) "modelled" else null) {
        if (air == null) {
            ShapeBadge("—", WeatherShapes.Clover4Leaf, 60.dp, colours.container, colours.onContainer, "No air quality reading")
            SmallText("Not available right now.", colours)
        } else {
            val (background, content) = aqiColours(air.usAqi)
            ShapeBadge(
                air.usAqi.toString(),
                WeatherShapes.Clover4Leaf,
                60.dp,
                background,
                content,
                "Air quality index ${air.usAqi}, ${aqiLabel(air.usAqi)}",
            )
            SmallText(aqiAdvice(air.usAqi), colours)
        }
    }
}

/** Badge colours by AQI category, from green to maroon. */
private fun aqiColours(aqi: Int): Pair<Color, Color> = when {
    aqi <= 50 -> Color(0xFFBDEBC4) to Color(0xFF00391A)
    aqi <= 100 -> Color(0xFFFFE08A) to Color(0xFF3B2F00)
    aqi <= 150 -> Color(0xFFFFCFA3) to Color(0xFF4A2500)
    aqi <= 200 -> Color(0xFFF2B8B5) to Color(0xFF601410)
    aqi <= 300 -> Color(0xFFE2C4EE) to Color(0xFF3F1150)
    else -> Color(0xFFD7A2AA) to Color(0xFF4A0A14)
}

@Composable
private fun HumidityTile(humidityPct: Int, dewPointC: Double?, colours: CardColours, modifier: Modifier) {
    Tile(
        Icons.Rounded.WaterDrop,
        "Humidity",
        colours,
        modifier,
        behind = {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(humidityPct.coerceIn(0, 100) / 100f)
                    .background(
                        colours.container.copy(alpha = 0.7f),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    ),
            )
        },
    ) {
        BigText("$humidityPct%", colours)
        if (dewPointC != null) SmallText("Dew point ${formatTemperature(dewPointC)}", colours)
    }
}

@Composable
private fun WindTile(windKmh: Double, fromDeg: Int, gustKmh: Double?, colours: CardColours, modifier: Modifier) {
    Tile(Icons.Rounded.Air, "Wind", colours, modifier) {
        val from = compassPoint(fromDeg)
        Box(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 6.dp)
                .size(58.dp)
                .border(3.dp, colours.container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // The arrow points where the wind blows to, the opposite of where it comes from.
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = "Wind from $from",
                tint = colours.primary,
                modifier = Modifier
                    .size(26.dp)
                    .rotate((fromDeg + 180) % 360f),
            )
        }
        val speed = windKmh.roundToInt()
        val text = buildString {
            append(if (speed < 1) "Calm" else "$speed km/h from $from")
            if (gustKmh != null) append("\nGusts ${gustKmh.roundToInt()} km/h")
        }
        SmallText(text, colours)
    }
}

@Composable
private fun RainfallTile(last24hMm: Double?, tomorrowMm: Double?, colours: CardColours, modifier: Modifier) {
    Tile(Icons.Rounded.Umbrella, "Rainfall", colours, modifier) {
        BigText(formatMm(last24hMm), colours)
        val text = buildString {
            append("Last 24 h.")
            if (tomorrowMm != null) append("\n${formatMm(tomorrowMm)} expected tomorrow.")
        }
        SmallText(text, colours)
    }
}

@Composable
private fun SunTile(sunrise: LocalTime, sunset: LocalTime, now: LocalTime, colours: CardColours, modifier: Modifier) {
    val daytime = !now.isBefore(sunrise) && now.isBefore(sunset)
    Tile(Icons.Rounded.WbTwilight, if (daytime) "Sunset" else "Sunrise", colours, modifier) {
        val progress = if (daytime) {
            Duration.between(sunrise, now).toMinutes().toFloat() /
                Duration.between(sunrise, sunset).toMinutes().coerceAtLeast(1).toFloat()
        } else {
            null
        }
        SunArc(progress, colours)
        BigText(formatTime(if (daytime) sunset else sunrise), colours, WeatherText.tileMedium)
        SmallText(if (daytime) "Sunrise ${formatTime(sunrise)}" else "Sunset ${formatTime(sunset)}", colours)
    }
}

/**
 * The sun's path as a quadratic curve (mockup viewBox 120 × 44). By day the part already
 * travelled and the sun itself are drawn in the primary colour; at night only the track.
 */
@Composable
private fun SunArc(progress: Float?, colours: CardColours) {
    Canvas(
        Modifier
            .padding(top = 6.dp, bottom = 2.dp)
            .size(width = 120.dp, height = 44.dp),
    ) {
        val sx = size.width / 120f
        val sy = size.height / 44f
        val p0 = Offset(4f * sx, 42f * sy)
        val p1 = Offset(60f * sx, -18f * sy)
        val p2 = Offset(116f * sx, 42f * sy)
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        drawPath(
            Path().apply {
                moveTo(p0.x, p0.y)
                quadraticTo(p1.x, p1.y, p2.x, p2.y)
            },
            colours.container,
            style = stroke,
        )
        if (progress != null) {
            val t = progress.coerceIn(0f, 1f)
            // de Casteljau split: the first part of the curve, from p0 to the sun.
            val control = p0 + (p1 - p0) * t
            val sun = (p0 * ((1 - t) * (1 - t))) + (p1 * (2 * (1 - t) * t)) + (p2 * (t * t))
            drawPath(
                Path().apply {
                    moveTo(p0.x, p0.y)
                    quadraticTo(control.x, control.y, sun.x, sun.y)
                },
                colours.primary,
                style = stroke,
            )
            drawCircle(colours.primary, radius = 6.dp.toPx(), center = sun)
        }
    }
}

@Preview(widthDp = 380, backgroundColor = 0xFF6FA6D9, showBackground = true)
@Composable
private fun DetailTilesDayPreview() = WeatherTheme {
    val s = SampleData.dayPartlyCloudy
    DetailTiles(s, LocalTime.of(12, 41), cardTint(s.now.condition, s.now.isDay))
}

@Preview(widthDp = 380, backgroundColor = 0xFF18222E, showBackground = true)
@Composable
private fun DetailTilesNightPreview() = WeatherTheme {
    val s = SampleData.nightRain
    DetailTiles(s, LocalTime.of(21, 15), cardTint(s.now.condition, s.now.isDay))
}
