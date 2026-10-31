package com.iwankurnia.weather.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.ForecastStep
import com.iwankurnia.weather.model.Now
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.ui.background.CardColours
import com.iwankurnia.weather.ui.background.cardTint
import com.iwankurnia.weather.ui.icons.conditionLabel
import com.iwankurnia.weather.ui.icons.weatherIconRes
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme

/** The forecast card: summary line, then a sideways-scrolling row of 3-hour pills. */
@Composable
internal fun ForecastCard(
    summary: String,
    now: Now,
    steps: List<ForecastStep>,
    colours: CardColours,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colours.surface)
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = summary,
            style = WeatherText.summary,
            color = colours.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(steps, key = { _, step -> step.time.toString() }) { index, step ->
                if (index == 0) {
                    // "Now" repeats the hero's values so the two never disagree.
                    ForecastPill(
                        label = "Now",
                        temperatureC = now.temperatureC,
                        condition = now.condition,
                        isDay = now.isDay,
                        rainChancePct = step.rainChancePct,
                        highlighted = true,
                        colours = colours,
                    )
                } else {
                    ForecastPill(
                        label = stepLabel(step.time),
                        temperatureC = step.temperatureC,
                        condition = step.condition,
                        isDay = step.isDay,
                        rainChancePct = step.rainChancePct,
                        highlighted = false,
                        colours = colours,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastPill(
    label: String,
    temperatureC: Double,
    condition: Condition,
    isDay: Boolean,
    rainChancePct: Int?,
    highlighted: Boolean,
    colours: CardColours,
) {
    val background = if (highlighted) colours.primary else colours.container
    val content = if (highlighted) colours.onPrimary else colours.onContainer
    val temperature = formatTemperature(temperatureC)
    val rain = rainChanceLabel(rainChancePct)
    val description = buildString {
        append("$label, $temperature, ${conditionLabel(condition, isDay)}")
        if (rain.isNotEmpty()) append(", $rain chance of rain")
    }
    Column(
        modifier = Modifier
            .width(50.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = WeatherText.pillLabel, color = content)
        Image(
            painter = painterResource(weatherIconRes(condition, isDay, useShowersIcon(condition, rainChancePct))),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(26.dp),
        )
        Text(
            text = temperature,
            style = WeatherText.pillTemperature,
            color = content,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        Text(
            text = rain,
            style = WeatherText.smallStrong,
            color = content,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(12.dp),
        )
    }
}

@Preview(widthDp = 380)
@Composable
private fun ForecastCardPreview() = WeatherTheme {
    val s = SampleData.dayPartlyCloudy
    ForecastCard(s.summary, s.now, s.steps, cardTint(s.now.condition, s.now.isDay))
}
