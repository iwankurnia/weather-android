package com.iwankurnia.weather.ui.background

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.iwankurnia.weather.model.Condition

/**
 * A top-to-bottom sky gradient with three stops, as in `mockups/backgrounds-day-night.html`.
 * Rain and storm use the richer falling-rain skies from `mockups/rain-storm.html`, and haze by
 * day uses the darker beige so white text stays readable.
 */
internal data class SkyGradient(
    val top: Color,
    val middle: Color,
    val bottom: Color,
    val middleStop: Float = 0.45f,
) {
    fun brush(): Brush = Brush.verticalGradient(0f to top, middleStop to middle, 1f to bottom)
}

internal fun skyGradient(condition: Condition, isDay: Boolean): SkyGradient = if (isDay) {
    when (condition) {
        Condition.CLEAR -> SkyGradient(Color(0xFF3D8FE0), Color(0xFF5EB0F2), Color(0xFF9FD2F7))
        Condition.PARTLY_CLOUDY -> SkyGradient(Color(0xFF4C88C9), Color(0xFF6FA6D9), Color(0xFFA8C9E6))
        Condition.CLOUDY -> SkyGradient(Color(0xFF7D8DA1), Color(0xFF97A5B6), Color(0xFFB6C0CC))
        Condition.RAIN -> SkyGradient(Color(0xFF46576A), Color(0xFF5D6F83), Color(0xFF7D8EA0), middleStop = 0.40f)
        Condition.THUNDERSTORM -> SkyGradient(Color(0xFF262B35), Color(0xFF363D4A), Color(0xFF4B5361))
        Condition.HAZE -> SkyGradient(Color(0xFF857B66), Color(0xFF9B917C), Color(0xFFB3AA96))
    }
} else {
    when (condition) {
        Condition.CLEAR -> SkyGradient(Color(0xFF0B1030), Color(0xFF1A2257), Color(0xFF2B3474))
        Condition.PARTLY_CLOUDY -> SkyGradient(Color(0xFF10162F), Color(0xFF222B52), Color(0xFF343E69))
        Condition.CLOUDY -> SkyGradient(Color(0xFF1B1F28), Color(0xFF2A303C), Color(0xFF3A4150))
        Condition.RAIN -> SkyGradient(Color(0xFF0D141C), Color(0xFF18222E), Color(0xFF243140))
        Condition.THUNDERSTORM -> SkyGradient(Color(0xFF050609), Color(0xFF0D1016), Color(0xFF171B23))
        Condition.HAZE -> SkyGradient(Color(0xFF1C1B18), Color(0xFF2C2A25), Color(0xFF3D3A33))
    }
}

/** Light cards with navy text: the `.day` theme in the mockups (clear and partly cloudy days). */
private val BlueDay = CardColours(
    surface = Color(0xEDF4F8FF), // rgba(244,248,255,.93)
    onSurface = Color(0xFF0F2A4A),
    subtle = Color(0xFF45607F),
    primary = Color(0xFF1F5FAE),
    onPrimary = Color.White,
    container = Color(0xFFD6E5FF),
    onContainer = Color(0xFF0F2A4A),
    onSky = Color.White,
)

/** Grey-blue cards for cloudy days (`.cloudy-d` override). */
private val GreyDay = BlueDay.copy(
    surface = Color(0xEDF6F7FA), // rgba(246,247,250,.93)
    onSurface = Color(0xFF1D2530),
    subtle = Color(0xFF4D5866),
    primary = Color(0xFF3D5A80),
    container = Color(0xFFDDE3EC),
    onContainer = Color(0xFF1D2530),
)

/** Rain and storm days use the falling-rain card surface (`.rx.rain-d`, `.rx.storm-d`). */
private val WetDay = GreyDay.copy(surface = Color(0xEBF0F3F7)) // rgba(240,243,247,.92)

/** Haze by day keeps the grey surface with warm accents (`.haze-d`). */
private val HazeDay = GreyDay.copy(
    primary = Color(0xFF7A6A45),
    container = Color(0xFFECE4D2),
    onContainer = Color(0xFF2E2614),
)

/** Dark navy cards with light text: the `.night` theme, shared by every night sky. */
private val Night = CardColours(
    surface = Color(0xE61C2234), // rgba(28,34,52,.9)
    onSurface = Color(0xFFE4E9F5),
    subtle = Color(0xFF9FADC4),
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0B1D3D),
    container = Color(0xFF2B3A58),
    onContainer = Color(0xFFD8E4FF),
    onSky = Color(0xFFEEF1FF),
)

/** Card, accent and on-sky text colours that suit the background for [condition] and [isDay]. */
fun cardTint(condition: Condition, isDay: Boolean): CardColours = if (!isDay) {
    Night
} else {
    when (condition) {
        Condition.CLEAR, Condition.PARTLY_CLOUDY -> BlueDay
        Condition.CLOUDY -> GreyDay
        Condition.RAIN, Condition.THUNDERSTORM -> WetDay
        Condition.HAZE -> HazeDay
    }
}
