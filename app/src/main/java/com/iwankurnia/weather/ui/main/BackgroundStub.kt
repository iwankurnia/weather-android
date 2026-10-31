package com.iwankurnia.weather.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.ui.background.CardColours

/*
 * TEMPORARY stand-in for `ui.background.WeatherBackground` and `ui.background.cardTint`
 * (plan Task 4). Plain gradients and card colours from the mockups, no animation.
 * Delete this file at integration and import the real ones.
 */

/** Card colours that suit the background for [condition] by day or night. */
internal fun cardTint(condition: Condition, isDay: Boolean): CardColours {
    if (!isDay) return nightCards
    return when (condition) {
        Condition.CLEAR, Condition.PARTLY_CLOUDY -> brightDayCards
        Condition.CLOUDY, Condition.RAIN, Condition.THUNDERSTORM -> greyDayCards
        Condition.HAZE -> greyDayCards.copy(
            primary = Color(0xFF7A6A45),
            container = Color(0xFFECE4D2),
            onContainer = Color(0xFF2E2614),
        )
    }
}

/** A still sky gradient for [condition] by day or night. */
@Composable
internal fun WeatherBackground(condition: Condition, isDay: Boolean, modifier: Modifier = Modifier) {
    val (top, middle, bottom) = skyColours(condition, isDay)
    Box(
        modifier.background(
            Brush.verticalGradient(0f to Color(top), 0.45f to Color(middle), 1f to Color(bottom)),
        ),
    )
}

private val brightDayCards = CardColours(
    surface = Color(0xEDF4F8FF),
    onSurface = Color(0xFF0F2A4A),
    subtle = Color(0xFF45607F),
    primary = Color(0xFF1F5FAE),
    onPrimary = Color.White,
    container = Color(0xFFD6E5FF),
    onContainer = Color(0xFF0F2A4A),
    onSky = Color.White,
)

private val greyDayCards = CardColours(
    surface = Color(0xEDF6F7FA),
    onSurface = Color(0xFF1D2530),
    subtle = Color(0xFF4D5866),
    primary = Color(0xFF3D5A80),
    onPrimary = Color.White,
    container = Color(0xFFDDE3EC),
    onContainer = Color(0xFF1D2530),
    onSky = Color.White,
)

private val nightCards = CardColours(
    surface = Color(0xE61C2234),
    onSurface = Color(0xFFE4E9F5),
    subtle = Color(0xFF9FADC4),
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0B1D3D),
    container = Color(0xFF2B3A58),
    onContainer = Color(0xFFD8E4FF),
    onSky = Color(0xFFEEF1FF),
)

private fun skyColours(condition: Condition, isDay: Boolean): Triple<Long, Long, Long> = if (isDay) {
    when (condition) {
        Condition.CLEAR -> Triple(0xFF3D8FE0, 0xFF5EB0F2, 0xFF9FD2F7)
        Condition.PARTLY_CLOUDY -> Triple(0xFF4C88C9, 0xFF6FA6D9, 0xFFA8C9E6)
        Condition.CLOUDY -> Triple(0xFF7D8DA1, 0xFF97A5B6, 0xFFB6C0CC)
        Condition.RAIN -> Triple(0xFF46576A, 0xFF5D6F83, 0xFF7D8EA0)
        Condition.THUNDERSTORM -> Triple(0xFF262B35, 0xFF363D4A, 0xFF4B5361)
        Condition.HAZE -> Triple(0xFF857B66, 0xFF9B917C, 0xFFB3AA96)
    }
} else {
    when (condition) {
        Condition.CLEAR -> Triple(0xFF0B1030, 0xFF1A2257, 0xFF2B3474)
        Condition.PARTLY_CLOUDY -> Triple(0xFF10162F, 0xFF222B52, 0xFF343E69)
        Condition.CLOUDY -> Triple(0xFF1B1F28, 0xFF2A303C, 0xFF3A4150)
        Condition.RAIN -> Triple(0xFF0D141C, 0xFF18222E, 0xFF243140)
        Condition.THUNDERSTORM -> Triple(0xFF050609, 0xFF0D1016, 0xFF171B23)
        Condition.HAZE -> Triple(0xFF1C1B18, 0xFF2C2A25, 0xFF3D3A33)
    }
}
