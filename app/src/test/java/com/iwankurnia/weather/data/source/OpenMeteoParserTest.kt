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
    fun openMeteo_nullRainChance_keepsRows() {
        val json = """
            {"hourly":{"time":["2026-11-01T00:00","2026-11-01T01:00"],
              "temperature_2m":[27.0,26.5],"apparent_temperature":[30.0,29.5],
              "relative_humidity_2m":[80,82],"weather_code":[2,3],
              "precipitation_probability":[null,40],"precipitation":[0.0,0.1],
              "visibility":[20000,null],"uv_index":[0.0,0.0],"dew_point_2m":[23.0,23.1],
              "wind_speed_10m":[5.0,6.0],"wind_direction_10m":[90,100],"wind_gusts_10m":[10.0,11.0],
              "is_day":[0,0]},
             "daily":{"time":["2026-11-01","2026-11-02"],
              "temperature_2m_min":[25.0,25.5],"temperature_2m_max":[33.0,32.5],
              "weather_code":[80,3],"precipitation_probability_max":[null,20],
              "sunrise":["2026-11-01T05:26","2026-11-02T05:26"],
              "sunset":["2026-11-01T17:46","2026-11-02T17:46"]}}
        """

        val forecast = OpenMeteoParser.parse(json, null)

        assertEquals(2, forecast.hours.size)
        assertNull(forecast.hours[0].rainChancePct)
        assertEquals(40, forecast.hours[1].rainChancePct)
        assertEquals(2, forecast.days.size)
        assertNull(forecast.days[0].rainChancePct)
        assertEquals(20, forecast.days[1].rainChancePct)
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
