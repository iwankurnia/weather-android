package com.iwankurnia.weather.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Values from the mockups, for previews and UI tests. */
object SampleData {
    private val today: LocalDate = LocalDate.of(2026, 11, 1)
    private val place = Place("Kebayoran Baru", -6.2297, 106.7997, "31.74.07.1004")

    private fun days(first: Condition) = listOf(
        DayForecast(today, 25.0, 34.0, first, 40, DataSource.BMKG),
        DayForecast(today.plusDays(1), 25.0, 33.0, Condition.RAIN, 70, DataSource.BMKG),
        DayForecast(today.plusDays(2), 24.0, 32.0, Condition.THUNDERSTORM, 80, DataSource.BMKG),
        DayForecast(today.plusDays(3), 25.0, 34.0, Condition.PARTLY_CLOUDY, 20, DataSource.OPEN_METEO),
        DayForecast(today.plusDays(4), 26.0, 35.0, Condition.CLEAR, 10, DataSource.OPEN_METEO),
        DayForecast(today.plusDays(5), 25.0, 33.0, Condition.RAIN, 50, DataSource.OPEN_METEO),
        DayForecast(today.plusDays(6), 25.0, 33.0, Condition.RAIN, 70, DataSource.OPEN_METEO),
        DayForecast(today.plusDays(7), 24.0, 32.0, Condition.THUNDERSTORM, 80, DataSource.OPEN_METEO),
        DayForecast(today.plusDays(8), 25.0, 34.0, Condition.HAZE, 10, DataSource.OPEN_METEO),
        DayForecast(today.plusDays(9), 25.0, 34.0, Condition.PARTLY_CLOUDY, 20, DataSource.OPEN_METEO),
    )

    private fun step(h: Int, dayOffset: Long, t: Double, c: Condition, day: Boolean, rain: Int?) =
        ForecastStep(LocalDateTime.of(today.plusDays(dayOffset), LocalTime.of(h, 0)), t, c, day, rain)

    val dayPartlyCloudy = WeatherSnapshot(
        place = place,
        now = Now(33.0, 39.0, 72, Condition.PARTLY_CLOUDY, 12.0, 315, isDay = true),
        todayHighC = 34.0,
        todayLowC = 25.0,
        steps = listOf(
            step(13, 0, 33.0, Condition.PARTLY_CLOUDY, true, 20),
            step(16, 0, 31.0, Condition.RAIN, true, 60),
            step(19, 0, 28.0, Condition.RAIN, false, 80),
            step(22, 0, 27.0, Condition.CLOUDY, false, 30),
            step(1, 1, 26.0, Condition.PARTLY_CLOUDY, false, null),
            step(4, 1, 25.0, Condition.PARTLY_CLOUDY, false, null),
        ),
        days = days(Condition.PARTLY_CLOUDY),
        details = Details(11.0, 27.0, 20.0, 0.0, 14.0, LocalTime.of(5, 31), LocalTime.of(17, 49)),
        air = AirQuality(156, "Jakarta Selatan", "Ministry of Environment", modelled = false),
        summary = "Rain likely from around 16:00. Very hot until then.",
        source = DataSource.BMKG,
        fetchedAt = Instant.parse("2026-11-01T05:40:00Z"),
    )

    val nightRain = WeatherSnapshot(
        place = place,
        now = Now(25.0, 27.0, 92, Condition.RAIN, 8.0, 225, isDay = false),
        todayHighC = 33.0,
        todayLowC = 24.0,
        steps = listOf(
            step(21, 0, 25.0, Condition.RAIN, false, 90),
            step(0, 1, 25.0, Condition.RAIN, false, 50),
            step(3, 1, 25.0, Condition.CLOUDY, false, 20),
            step(6, 1, 25.0, Condition.PARTLY_CLOUDY, true, null),
            step(9, 1, 29.0, Condition.PARTLY_CLOUDY, true, null),
            step(12, 1, 32.0, Condition.CLEAR, true, null),
        ),
        days = days(Condition.RAIN),
        details = Details(0.0, 25.0, 15.0, 18.0, 14.0, LocalTime.of(5, 31), LocalTime.of(17, 49)),
        air = AirQuality(98, null, "Open-Meteo (CAMS)", modelled = true),
        summary = "Rain easing after midnight.",
        source = DataSource.BMKG,
        fetchedAt = Instant.parse("2026-11-01T14:10:00Z"),
    )
}
