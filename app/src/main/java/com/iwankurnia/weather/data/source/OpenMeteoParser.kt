package com.iwankurnia.weather.data.source

import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/** Parses the Open-Meteo forecast and air-quality responses. */
object OpenMeteoParser {
    /** The client asks for ten forecast days; any extra leading days are the requested past day. */
    private const val FORECAST_DAYS = 10

    /**
     * Parses [forecastJson] (throws when it is not a forecast) and, if given, [airJson].
     *
     * Air quality is optional: a missing or unreadable [airJson] only leaves `modelledUsAqi` null.
     * Hours and days missing a required value are skipped. Only visibility may be absent.
     */
    fun parse(forecastJson: String, airJson: String?): OpenMeteoForecast {
        val dto = sourceJson.decodeFromString<ForecastDto>(forecastJson)
        return OpenMeteoForecast(
            hours = parseHours(dto.hourly),
            days = parseDays(dto.daily).takeLast(FORECAST_DAYS),
            modelledUsAqi = airJson?.let(::parseAir),
        )
    }

    private fun parseHours(h: HourlyDto): List<OmHour> = h.time.indices.mapNotNull { i ->
        OmHour(
            time = catchingOrNull { LocalDateTime.parse(h.time[i]) } ?: return@mapNotNull null,
            temperatureC = h.temperature.at(i) ?: return@mapNotNull null,
            apparentC = h.apparent.at(i) ?: return@mapNotNull null,
            humidityPct = h.humidity.at(i)?.roundToInt() ?: return@mapNotNull null,
            weatherCode = h.weatherCode.at(i)?.roundToInt() ?: return@mapNotNull null,
            rainChancePct = h.rainChance.at(i)?.roundToInt() ?: return@mapNotNull null,
            precipitationMm = h.precipitation.at(i) ?: return@mapNotNull null,
            visibilityM = h.visibility.at(i),
            uvIndex = h.uvIndex.at(i) ?: return@mapNotNull null,
            dewPointC = h.dewPoint.at(i) ?: return@mapNotNull null,
            windKmh = h.windSpeed.at(i) ?: return@mapNotNull null,
            windFromDeg = h.windDirection.at(i)?.roundToInt() ?: return@mapNotNull null,
            gustKmh = h.windGusts.at(i) ?: return@mapNotNull null,
            isDay = (h.isDay.at(i) ?: return@mapNotNull null) >= 0.5,
        )
    }

    private fun parseDays(d: DailyDto): List<OmDay> = d.time.indices.mapNotNull { i ->
        OmDay(
            date = catchingOrNull { LocalDate.parse(d.time[i]) } ?: return@mapNotNull null,
            lowC = d.min.at(i) ?: return@mapNotNull null,
            highC = d.max.at(i) ?: return@mapNotNull null,
            weatherCode = d.weatherCode.at(i)?.roundToInt() ?: return@mapNotNull null,
            rainChancePct = d.rainChance.at(i)?.roundToInt() ?: return@mapNotNull null,
            sunrise = d.sunrise.timeAt(i) ?: return@mapNotNull null,
            sunset = d.sunset.timeAt(i) ?: return@mapNotNull null,
        )
    }

    private fun parseAir(json: String): Int? = catchingOrNull {
        sourceJson.parseToJsonElement(json).asObject()?.get("current").asObject()?.get("us_aqi").integer()
    }

    private fun List<Double?>.at(i: Int): Double? = getOrNull(i)?.takeIf { it.isFinite() }

    private fun List<String?>.timeAt(i: Int) =
        getOrNull(i)?.let { catchingOrNull { LocalDateTime.parse(it).toLocalTime() } }

    private fun Double.roundToInt(): Int = Math.round(this).toInt()

    @Serializable
    private data class ForecastDto(val hourly: HourlyDto, val daily: DailyDto)

    @Serializable
    private data class HourlyDto(
        val time: List<String> = emptyList(),
        @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
        @SerialName("apparent_temperature") val apparent: List<Double?> = emptyList(),
        @SerialName("relative_humidity_2m") val humidity: List<Double?> = emptyList(),
        @SerialName("weather_code") val weatherCode: List<Double?> = emptyList(),
        @SerialName("precipitation_probability") val rainChance: List<Double?> = emptyList(),
        val precipitation: List<Double?> = emptyList(),
        val visibility: List<Double?> = emptyList(),
        @SerialName("uv_index") val uvIndex: List<Double?> = emptyList(),
        @SerialName("dew_point_2m") val dewPoint: List<Double?> = emptyList(),
        @SerialName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
        @SerialName("wind_direction_10m") val windDirection: List<Double?> = emptyList(),
        @SerialName("wind_gusts_10m") val windGusts: List<Double?> = emptyList(),
        @SerialName("is_day") val isDay: List<Double?> = emptyList(),
    )

    @Serializable
    private data class DailyDto(
        val time: List<String> = emptyList(),
        @SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
        @SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
        @SerialName("weather_code") val weatherCode: List<Double?> = emptyList(),
        @SerialName("precipitation_probability_max") val rainChance: List<Double?> = emptyList(),
        val sunrise: List<String?> = emptyList(),
        val sunset: List<String?> = emptyList(),
    )
}
