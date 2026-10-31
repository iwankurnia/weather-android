package com.iwankurnia.weather.ui.main

import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.DataSource
import com.iwankurnia.weather.model.WeatherSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Text formatting for the main screen. Pure functions so they can be unit-tested. */

private val ukLocale: Locale = Locale.UK
private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", ukLocale)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", ukLocale)
private val hourFormat = DateTimeFormatter.ofPattern("HH", ukLocale)

/** Whole degrees with a degree sign, for example "33°". Never shows "-0°". */
fun formatTemperature(celsius: Double): String {
    val whole = celsius.roundToInt()
    return "${if (whole == 0) 0 else whole}°"
}

/** "Sunday, 1 November". */
fun formatDate(date: LocalDate): String = date.format(dateFormat)

/** 24-hour "05:31". */
fun formatTime(time: LocalTime): String = time.format(timeFormat)

/** An instant as a 24-hour clock time in [zone], for example "12:40". */
fun formatClock(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    instant.atZone(zone).format(timeFormat)

/** Hour label of a forecast pill, for example "16". */
fun stepLabel(time: LocalDateTime): String = time.format(hourFormat)

/** "Today" for [today], otherwise the short weekday ("Mon"). */
fun dayLabel(date: LocalDate, today: LocalDate): String =
    if (date == today) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, ukLocale)

/** US EPA AQI category. */
fun aqiLabel(aqi: Int): String = when {
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy for sensitive groups"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very unhealthy"
    else -> "Hazardous"
}

/** Category plus a short piece of advice, for the air-quality tile. */
fun aqiAdvice(aqi: Int): String = aqiLabel(aqi) + when {
    aqi <= 50 -> ". Fine to be outside."
    aqi <= 100 -> ". Fine for most people."
    aqi <= 150 -> ". Take it easy outdoors."
    aqi <= 200 -> ". Mask outdoors."
    aqi <= 300 -> ". Stay indoors if you can."
    else -> ". Stay indoors."
}

/** WHO UV index category, on the rounded index. */
fun uvLabel(uv: Double): String = when (uv.roundToInt()) {
    in Int.MIN_VALUE..2 -> "Low"
    in 3..5 -> "Moderate"
    in 6..7 -> "High"
    in 8..10 -> "Very high"
    else -> "Extreme"
}

/** Category plus advice for the UV tile; "None at night." after dark. */
fun uvAdvice(uv: Double?, isDay: Boolean): String {
    if (uv == null) return "Not available."
    if (!isDay && uv.roundToInt() == 0) return "None at night."
    return uvLabel(uv) + when (uv.roundToInt()) {
        in Int.MIN_VALUE..2 -> ". No protection needed."
        in 3..5 -> ". Sunscreen if you’re out for long."
        in 6..7 -> ". Cover up around midday."
        in 8..10 -> ". Seek shade around midday."
        else -> ". Avoid the midday sun."
    }
}

private val compassPoints = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/** Eight-point compass name for a bearing in degrees. */
fun compassPoint(degrees: Int): String {
    val normalised = ((degrees % 360) + 360) % 360
    return compassPoints[((normalised + 22.5) / 45.0).toInt() % 8]
}

/** Rainfall: one decimal below 10 mm when it isn't whole, else whole millimetres. */
fun formatMm(mm: Double?): String = when {
    mm == null -> "—"
    mm < 10 && abs(mm - mm.roundToInt()) >= 0.05 -> String.format(ukLocale, "%.1f mm", mm)
    else -> "${mm.roundToInt()} mm"
}

/** Chance of rain worth showing ("60%"); blank below 30% so the rows stay calm. */
fun rainChanceLabel(pct: Int?): String = if (pct != null && pct >= 30) "$pct%" else ""

/** Light rain (under 70% chance) uses the sun or moon with showers icon. */
fun useShowersIcon(condition: Condition, rainChancePct: Int?): Boolean =
    condition == Condition.RAIN && rainChancePct != null && rainChancePct < 70

/**
 * The credits line at the bottom of the main screen, for example
 * "Updated 12:40 · Weather: BMKG · Open-Meteo.com · Air quality: WAQI".
 * BMKG is only credited when its data is used; WAQI names the originating agency when known.
 */
fun creditsLine(snapshot: WeatherSnapshot, zone: ZoneId = ZoneId.systemDefault()): String {
    val weather = when (snapshot.source) {
        DataSource.BMKG -> "Weather: BMKG · Open-Meteo.com"
        DataSource.OPEN_METEO -> "Weather: Open-Meteo.com"
    }
    val air = snapshot.air
    val airCredit = when {
        air == null -> null
        air.modelled -> "Air quality: Open-Meteo (CAMS)"
        air.attribution.isNullOrBlank() -> "Air quality: WAQI"
        else -> "Air quality: WAQI, ${air.attribution}"
    }
    return listOfNotNull("Updated ${formatClock(snapshot.fetchedAt, zone)}", weather, airCredit)
        .joinToString(" · ")
}
