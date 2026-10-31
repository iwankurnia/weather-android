package com.iwankurnia.weather.data.source

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One BMKG 3-hour forecast step. `local` is Asia/Jakarta wall-clock time, as BMKG sends it. */
data class BmkgStep(
    val local: LocalDateTime,
    val temperatureC: Double,
    val humidityPct: Int,
    val weatherCode: Int,
    val windKmh: Double,
    val windFromDeg: Int,
    val rainMm: Double,
)

data class BmkgForecast(
    val kelurahan: String,
    val kecamatan: String,
    val latitude: Double,
    val longitude: Double,
    /** All steps for the 3 days, in time order. */
    val steps: List<BmkgStep>,
)

data class OmHour(
    val time: LocalDateTime,
    val temperatureC: Double,
    val apparentC: Double,
    val humidityPct: Int,
    val weatherCode: Int,
    val rainChancePct: Int,
    val precipitationMm: Double,
    val visibilityM: Double?,
    val uvIndex: Double,
    val dewPointC: Double,
    val windKmh: Double,
    val windFromDeg: Int,
    val gustKmh: Double,
    val isDay: Boolean,
)

data class OmDay(
    val date: LocalDate,
    val lowC: Double,
    val highC: Double,
    val weatherCode: Int,
    val rainChancePct: Int,
    val sunrise: LocalTime,
    val sunset: LocalTime,
)

data class OpenMeteoForecast(
    /** Hourly, Asia/Jakarta time, including the past day. */
    val hours: List<OmHour>,
    /** Ten days starting today. */
    val days: List<OmDay>,
    /** Current US AQI from the CAMS model, used only when WAQI has nothing. */
    val modelledUsAqi: Int?,
)

data class Kelurahan(
    val code: String,
    val name: String,
    val kecamatan: String,
    val latitude: Double,
    val longitude: Double,
)
