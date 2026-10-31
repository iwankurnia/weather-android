package com.iwankurnia.weather.data.source

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoParserTest {
    private val forecastJson = fixture("openmeteo_forecast.json")

    @Test
    fun openMeteo_parsesHourlyDailyAndAir() {
        val forecast = OpenMeteoParser.parse(forecastJson, fixture("openmeteo_air.json"))

        assertTrue(forecast.hours.size >= 24)
        assertEquals(264, forecast.hours.size)
        assertEquals(LocalDateTime.of(2026, 10, 31, 0, 0), forecast.hours.first().time)

        val afternoon = forecast.hours.single { it.time == LocalDateTime.of(2026, 11, 1, 13, 0) }
        assertEquals(30.1, afternoon.temperatureC, 1e-9)
        assertEquals(34.9, afternoon.apparentC, 1e-9)
        assertEquals(68, afternoon.humidityPct)
        assertEquals(53, afternoon.weatherCode)
        assertEquals(53, afternoon.rainChancePct)
        assertEquals(0.8, afternoon.precipitationMm, 1e-9)
        assertEquals(9460.0, afternoon.visibilityM!!, 1e-9)
        assertEquals(8.05, afternoon.uvIndex, 1e-9)
        assertEquals(23.5, afternoon.dewPointC, 1e-9)
        assertEquals(10.6, afternoon.windKmh, 1e-9)
        assertEquals(9, afternoon.windFromDeg)
        assertEquals(33.1, afternoon.gustKmh, 1e-9)
        assertTrue(afternoon.isDay)
        assertFalse(forecast.hours.first().isDay)

        assertEquals(10, forecast.days.size)
        val today = forecast.days.first()
        assertEquals(LocalDate.of(2026, 11, 1), today.date)
        assertEquals(25.4, today.lowC, 1e-9)
        assertEquals(33.5, today.highC, 1e-9)
        assertEquals(81, today.weatherCode)
        assertEquals(93, today.rainChancePct)
        assertEquals(LocalTime.of(5, 26), today.sunrise)
        assertEquals(LocalTime.of(17, 46), today.sunset)
        assertEquals(LocalDate.of(2026, 11, 10), forecast.days.last().date)

        assertEquals(163, forecast.modelledUsAqi)
    }

    @Test
    fun openMeteo_missingOrBadAir_givesNullAqi() {
        assertNull(OpenMeteoParser.parse(forecastJson, null).modelledUsAqi)
        assertNull(OpenMeteoParser.parse(forecastJson, "{\"error\":true,\"reason\":\"x\"}").modelledUsAqi)
        assertNull(OpenMeteoParser.parse(forecastJson, "not json").modelledUsAqi)
    }

    @Test(expected = Exception::class)
    fun openMeteo_malformedForecast_throws() {
        OpenMeteoParser.parse("{\"error\":true,\"reason\":\"Cannot initialize\"}", null)
    }
}
