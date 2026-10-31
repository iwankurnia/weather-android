package com.iwankurnia.weather.ui.icons

import androidx.annotation.DrawableRes
import com.iwankurnia.weather.R
import com.iwankurnia.weather.model.Condition

/**
 * The weather icon for a condition. Vector drawables so both Compose (painterResource)
 * and Glance widgets (ImageProvider) use the same artwork.
 * [showers] picks the sun/moon-with-rain icon for light rain in the forecast row.
 */
@DrawableRes
fun weatherIconRes(condition: Condition, isDay: Boolean, showers: Boolean = false): Int = when (condition) {
    Condition.CLEAR -> if (isDay) R.drawable.wx_clear_day else R.drawable.wx_clear_night
    Condition.PARTLY_CLOUDY -> if (isDay) R.drawable.wx_partly_day else R.drawable.wx_partly_night
    Condition.CLOUDY -> R.drawable.wx_cloudy
    Condition.RAIN -> when {
        !showers -> R.drawable.wx_rain
        isDay -> R.drawable.wx_showers_day
        else -> R.drawable.wx_showers_night
    }
    Condition.THUNDERSTORM -> R.drawable.wx_storm
    Condition.HAZE -> if (isDay) R.drawable.wx_haze_day else R.drawable.wx_haze_night
}

/** Short English label for a condition, as shown on screen and in widgets. */
fun conditionLabel(condition: Condition, isDay: Boolean): String = when (condition) {
    Condition.CLEAR -> if (isDay) "Sunny" else "Clear"
    Condition.PARTLY_CLOUDY -> "Partly cloudy"
    Condition.CLOUDY -> "Cloudy"
    Condition.RAIN -> "Rain"
    Condition.THUNDERSTORM -> "Thunderstorm"
    Condition.HAZE -> "Haze"
}
