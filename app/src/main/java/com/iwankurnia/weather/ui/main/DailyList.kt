package com.iwankurnia.weather.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iwankurnia.weather.model.DayForecast
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.ui.background.CardColours
import com.iwankurnia.weather.ui.icons.conditionLabel
import com.iwankurnia.weather.ui.icons.weatherIconRes
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme
import java.time.LocalDate

private val OuterCorner = 24.dp
private val InnerCorner = 6.dp

/** The 10-day forecast as one Settings-style group: 24 dp outer corners, 6 dp inner, 2 dp gaps. */
@Composable
internal fun DailyList(days: List<DayForecast>, today: LocalDate, colours: CardColours, modifier: Modifier = Modifier) {
    if (days.isEmpty()) return
    val coldest = days.minOf { it.lowC }
    val hottest = days.maxOf { it.highC }
    Column(modifier.fillMaxWidth()) {
        SectionHeader(Icons.Rounded.CalendarMonth, "10-day forecast", colours.onSky)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            days.forEachIndexed { index, day ->
                DayRow(
                    day = day,
                    label = dayLabel(day.date, today),
                    coldest = coldest,
                    hottest = hottest,
                    shape = groupedShape(index, days.size),
                    colours = colours,
                )
            }
        }
    }
}

/** Corner shape for row [index] of [count] in a grouped list. */
internal fun groupedShape(index: Int, count: Int, outer: Dp = OuterCorner, inner: Dp = InnerCorner) =
    RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )

@Composable
private fun DayRow(
    day: DayForecast,
    label: String,
    coldest: Double,
    hottest: Double,
    shape: RoundedCornerShape,
    colours: CardColours,
) {
    val rain = rainChanceLabel(day.rainChancePct)
    val low = formatTemperature(day.lowC)
    val high = formatTemperature(day.highC)
    val description = buildString {
        append("$label, ${conditionLabel(day.condition, true)}, low $low, high $high")
        if (rain.isNotEmpty()) append(", $rain chance of rain")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colours.surface)
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = WeatherText.rowDay, color = colours.onSurface, modifier = Modifier.width(48.dp))
        Column(Modifier.width(34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(weatherIconRes(day.condition, true, useShowersIcon(day.condition, day.rainChancePct))),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            if (rain.isNotEmpty()) Text(rain, style = WeatherText.smallStrong, color = colours.primary)
        }
        Text(
            text = low,
            style = WeatherText.row,
            color = colours.subtle,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp),
        )
        RangeBar(
            from = ((day.lowC - coldest) / (hottest - coldest).coerceAtLeast(1.0)).toFloat(),
            to = ((day.highC - coldest) / (hottest - coldest).coerceAtLeast(1.0)).toFloat(),
            track = colours.container,
            fill = colours.primary,
            modifier = Modifier.weight(1f),
        )
        Text(high, style = WeatherText.row, color = colours.onSurface, modifier = Modifier.width(28.dp))
    }
}

/** A 6 dp track with the day's low-to-high span filled, on the scale of the whole list. */
@Composable
private fun RangeBar(from: Float, to: Float, track: Color, fill: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = radius)
        val start = from.coerceIn(0f, 1f) * size.width
        val end = to.coerceIn(0f, 1f) * size.width
        drawRoundRect(
            color = fill,
            topLeft = Offset(start, 0f),
            size = Size((end - start).coerceAtLeast(size.height), size.height),
            cornerRadius = radius,
        )
    }
}

/** Small heading drawn straight on the sky, such as "10-day forecast". */
@Composable
internal fun SectionHeader(icon: ImageVector, text: String, colour: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colour, modifier = Modifier.size(16.dp))
        Text(text, style = WeatherText.sectionHeader, color = colour)
    }
}

@Preview(widthDp = 380, backgroundColor = 0xFF6FA6D9, showBackground = true)
@Composable
private fun DailyListPreview() = WeatherTheme {
    val s = SampleData.dayPartlyCloudy
    Column {
        DailyList(s.days, s.days.first().date, cardTint(s.now.condition, s.now.isDay))
        Spacer(Modifier.height(8.dp))
    }
}
