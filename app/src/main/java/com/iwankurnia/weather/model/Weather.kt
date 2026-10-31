package com.iwankurnia.weather.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The six background and icon families. Temperature never changes which one is used. */
enum class Condition { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, THUNDERSTORM, HAZE }

/** Where the forecast in a snapshot (or one day of it) came from. */
enum class DataSource { BMKG, OPEN_METEO }

data class Place(
    val areaName: String,
    val latitude: Double,
    val longitude: Double,
    /** BMKG adm4 code of the nearest kelurahan, or null when outside coverage. */
    val kelurahanCode: String?,
)

data class Now(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPct: Int,
    val condition: Condition,
    val windKmh: Double,
    val windFromDeg: Int,
    val isDay: Boolean,
)

/** One pill in the forecast row: BMKG 3-hour steps, or every third Open-Meteo hour as a fallback. */
data class ForecastStep(
    val time: LocalDateTime,
    val temperatureC: Double,
    val condition: Condition,
    val isDay: Boolean,
    val rainChancePct: Int?,
)

data class DayForecast(
    val date: LocalDate,
    val lowC: Double,
    val highC: Double,
    val condition: Condition,
    val rainChancePct: Int?,
    val source: DataSource,
)

data class AirQuality(
    /** US EPA AQI scale. */
    val usAqi: Int,
    val stationName: String?,
    /** Originating agency to credit, when the feed gives one. */
    val attribution: String?,
    /** True when it comes from Open-Meteo's CAMS model instead of a station. */
    val modelled: Boolean,
)

data class Details(
    val uvIndex: Double?,
    val dewPointC: Double?,
    val gustKmh: Double?,
    val rainLast24hMm: Double?,
    val rainTomorrowMm: Double?,
    val sunrise: LocalTime,
    val sunset: LocalTime,
)

/** Everything the main screen and the widgets show. Built only by SnapshotMerger. */
data class WeatherSnapshot(
    val place: Place,
    val now: Now,
    val todayHighC: Double,
    val todayLowC: Double,
    val steps: List<ForecastStep>,
    val days: List<DayForecast>,
    val details: Details,
    val air: AirQuality?,
    val summary: String,
    val source: DataSource,
    val fetchedAt: Instant,
)
