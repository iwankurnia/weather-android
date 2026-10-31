package com.iwankurnia.weather.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * "Feels like" in °C: the NOAA/NWS heat index for [temperatureC] and [humidityPct].
 *
 * Follows the NWS heat-index page: Steadman's simple formula first; when that result averaged
 * with the temperature is 80 °F or more, the Rothfusz regression with its low- and high-humidity
 * adjustments is used instead. Never returns less than the air temperature.
 */
fun feelsLikeC(temperatureC: Double, humidityPct: Int): Double {
    val t = temperatureC * 9.0 / 5.0 + 32.0
    val rh = humidityPct.coerceIn(0, 100).toDouble()

    val simple = 0.5 * (t + 61.0 + (t - 68.0) * 1.2 + rh * 0.094)
    val heatIndexF = if ((simple + t) / 2.0 < 80.0) simple else rothfusz(t, rh)

    return max((heatIndexF - 32.0) * 5.0 / 9.0, temperatureC)
}

/** Rothfusz regression in °F, with the NWS humidity adjustments. */
private fun rothfusz(t: Double, rh: Double): Double {
    var hi = -42.379 + 2.04901523 * t + 10.14333127 * rh -
        0.22475541 * t * rh - 0.00683783 * t * t - 0.05481717 * rh * rh +
        0.00122874 * t * t * rh + 0.00085282 * t * rh * rh - 0.00000199 * t * t * rh * rh

    if (rh < 13.0 && t in 80.0..112.0) {
        hi -= ((13.0 - rh) / 4.0) * sqrt((17.0 - abs(t - 95.0)) / 17.0)
    } else if (rh > 85.0 && t in 80.0..87.0) {
        hi += ((rh - 85.0) / 10.0) * ((87.0 - t) / 5.0)
    }
    return hi
}
