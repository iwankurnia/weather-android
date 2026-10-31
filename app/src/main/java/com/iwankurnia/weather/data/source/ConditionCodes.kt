package com.iwankurnia.weather.data.source

import com.iwankurnia.weather.model.Condition

/** Below this visibility, a dry hour counts as haze in the Open-Meteo fallback. */
private const val HAZE_VISIBILITY_M = 5_000.0

/** Maps a BMKG weather code to one of the six conditions. Unknown codes become [Condition.CLOUDY]. */
fun bmkgCondition(code: Int): Condition = when (code) {
    0 -> Condition.CLEAR
    1, 2 -> Condition.PARTLY_CLOUDY
    3, 4 -> Condition.CLOUDY
    5, 10, 45 -> Condition.HAZE
    60, 61, 63, 80 -> Condition.RAIN
    95, 97 -> Condition.THUNDERSTORM
    else -> Condition.CLOUDY
}

/**
 * Maps a WMO weather code from Open-Meteo to one of the six conditions.
 *
 * Fog codes 45 and 48 are haze, and so is any dry hour with visibility under 5 km.
 * Rain and thunderstorm codes always win. Other codes become [Condition.CLOUDY].
 */
fun wmoCondition(code: Int, visibilityM: Double?, raining: Boolean): Condition = when (code) {
    in 95..99 -> Condition.THUNDERSTORM
    in 51..67, in 80..82 -> Condition.RAIN
    45, 48 -> Condition.HAZE
    else -> when {
        !raining && visibilityM != null && visibilityM < HAZE_VISIBILITY_M -> Condition.HAZE
        code == 0 -> Condition.CLEAR
        code == 1 || code == 2 -> Condition.PARTLY_CLOUDY
        else -> Condition.CLOUDY
    }
}
