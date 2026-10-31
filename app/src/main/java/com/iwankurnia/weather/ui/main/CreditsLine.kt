package com.iwankurnia.weather.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.background.CardColours
import com.iwankurnia.weather.ui.background.cardTint
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme
import java.time.ZoneId

/** "Updated hh:mm · Weather: BMKG · Open-Meteo.com · Air quality: WAQI" in a small pill. */
@Composable
internal fun CreditsLine(
    snapshot: WeatherSnapshot,
    colours: CardColours,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = creditsLine(snapshot, zone),
            style = WeatherText.credits,
            color = colours.subtle,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colours.surface)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Preview(widthDp = 380, backgroundColor = 0xFF6FA6D9, showBackground = true)
@Composable
private fun CreditsLinePreview() = WeatherTheme {
    val s = SampleData.dayPartlyCloudy
    CreditsLine(s, cardTint(s.now.condition, s.now.isDay), zone = ZoneId.of("Asia/Jakarta"))
}
