package com.iwankurnia.weather.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iwankurnia.weather.model.Now
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.ui.background.CardColours
import com.iwankurnia.weather.ui.background.cardTint
import com.iwankurnia.weather.ui.icons.conditionLabel
import com.iwankurnia.weather.ui.icons.weatherIconRes
import com.iwankurnia.weather.ui.theme.WeatherShapes
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme

/** Big temperature, condition and today's range, with "feels like" in a cookie badge. */
@Composable
internal fun Hero(now: Now, highC: Double, lowC: Double, colours: CardColours, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = formatTemperature(now.temperatureC),
                style = WeatherText.heroTemperature,
                color = colours.onSky,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Image(
                    painter = painterResource(weatherIconRes(now.condition, now.isDay)),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    text = conditionLabel(now.condition, now.isDay),
                    style = WeatherText.condition,
                    color = colours.onSky,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "High ${formatTemperature(highC)} · Low ${formatTemperature(lowC)}",
                style = WeatherText.highLow,
                color = colours.onSky.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        FeelsLikeBadge(now.feelsLikeC, colours)
    }
}

/** "Feels like 39°" in the primary colour, inside the nine-sided cookie. */
@Composable
internal fun FeelsLikeBadge(feelsLikeC: Double, colours: CardColours, modifier: Modifier = Modifier) {
    val value = formatTemperature(feelsLikeC)
    Box(
        modifier = modifier
            .size(100.dp)
            .background(colours.primary, WeatherShapes.Cookie9Sided)
            .clearAndSetSemantics { contentDescription = "Feels like $value" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Feels like", style = WeatherText.feelsLabel, color = colours.onPrimary)
            Text(value, style = WeatherText.feelsValue, color = colours.onPrimary)
        }
    }
}

@Preview(widthDp = 380, backgroundColor = 0xFF4C88C9, showBackground = true)
@Composable
private fun HeroDayPreview() = WeatherTheme {
    val s = SampleData.dayPartlyCloudy
    Hero(s.now, s.todayHighC, s.todayLowC, cardTint(s.now.condition, s.now.isDay))
}
