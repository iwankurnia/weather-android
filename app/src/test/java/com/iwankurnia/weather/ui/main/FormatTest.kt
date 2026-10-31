package com.iwankurnia.weather.ui.main

import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.DataSource
import com.iwankurnia.weather.model.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class FormatTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    @Test
    fun format_temperature() {
        assertEquals("33°", formatTemperature(33.4))
        assertEquals("0°", formatTemperature(-0.4))
        assertEquals("34°", formatTemperature(33.5))
        assertEquals("-3°", formatTemperature(-3.2))
    }

    @Test
    fun format_date() {
        assertEquals("Sunday, 1 November", formatDate(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun format_times() {
        assertEquals("05:31", formatTime(LocalTime.of(5, 31)))
        assertEquals("12:40", formatClock(Instant.parse("2026-11-01T05:40:00Z"), jakarta))
        assertEquals("16", stepLabel(LocalDateTime.of(2026, 11, 1, 16, 0)))
        assertEquals("01", stepLabel(LocalDateTime.of(2026, 11, 2, 1, 0)))
    }

    @Test
    fun format_dayLabel() {
        val today = LocalDate.of(2026, 11, 1)
        assertEquals("Today", dayLabel(today, today))
        assertEquals("Mon", dayLabel(today.plusDays(1), today))
        assertEquals("Sat", dayLabel(today.plusDays(6), today))
    }

    @Test
    fun format_aqiLabel() {
        assertEquals("Good", aqiLabel(0))
        assertEquals("Good", aqiLabel(50))
        assertEquals("Moderate", aqiLabel(51))
        assertEquals("Moderate", aqiLabel(100))
        assertEquals("Unhealthy for sensitive groups", aqiLabel(101))
        assertEquals("Unhealthy for sensitive groups", aqiLabel(150))
        assertEquals("Unhealthy", aqiLabel(151))
        assertEquals("Unhealthy", aqiLabel(200))
        assertEquals("Very unhealthy", aqiLabel(201))
        assertEquals("Very unhealthy", aqiLabel(300))
        assertEquals("Hazardous", aqiLabel(301))
        assertEquals("Hazardous", aqiLabel(500))
    }

    @Test
    fun format_uvLabel() {
        assertEquals("Low", uvLabel(0.0))
        assertEquals("Low", uvLabel(2.0))
        assertEquals("Moderate", uvLabel(3.0))
        assertEquals("Moderate", uvLabel(5.0))
        assertEquals("High", uvLabel(6.0))
        assertEquals("High", uvLabel(7.0))
        assertEquals("Very high", uvLabel(8.0))
        assertEquals("Very high", uvLabel(10.0))
        assertEquals("Extreme", uvLabel(11.0))
        assertEquals("Extreme", uvLabel(14.2))
    }

    @Test
    fun format_advice() {
        assertEquals("Extreme. Avoid the midday sun.", uvAdvice(11.0, isDay = true))
        assertEquals("None at night.", uvAdvice(0.0, isDay = false))
        assertEquals("Unhealthy. Mask outdoors.", aqiAdvice(156))
    }

    @Test
    fun format_windAndRain() {
        assertEquals("NW", compassPoint(315))
        assertEquals("N", compassPoint(350))
        assertEquals("N", compassPoint(0))
        assertEquals("SE", compassPoint(135))
        assertEquals("0 mm", formatMm(0.0))
        assertEquals("14 mm", formatMm(14.0))
        assertEquals("2.4 mm", formatMm(2.4))
        assertEquals("—", formatMm(null))
    }

    @Test
    fun format_rainChanceAndShowers() {
        assertEquals("60%", rainChanceLabel(60))
        assertEquals("30%", rainChanceLabel(30))
        assertEquals("", rainChanceLabel(20))
        assertEquals("", rainChanceLabel(null))
        assertTrue(useShowersIcon(Condition.RAIN, 60))
        assertFalse(useShowersIcon(Condition.RAIN, 80))
        assertFalse(useShowersIcon(Condition.RAIN, null))
        assertFalse(useShowersIcon(Condition.CLOUDY, 30))
    }

    @Test
    fun format_creditsLine() {
        assertEquals(
            "Updated 12:40 · Weather: BMKG · Open-Meteo.com · Air quality: WAQI, Ministry of Environment",
            creditsLine(SampleData.dayPartlyCloudy, jakarta),
        )
        val waqiNoAgency = SampleData.dayPartlyCloudy.copy(air = AirQuality(80, "Station", null, false))
        assertEquals(
            "Updated 12:40 · Weather: BMKG · Open-Meteo.com · Air quality: WAQI",
            creditsLine(waqiNoAgency, jakarta),
        )
        val openMeteoOnly = SampleData.nightRain.copy(source = DataSource.OPEN_METEO)
        assertEquals(
            "Updated 21:10 · Weather: Open-Meteo.com · Air quality: Open-Meteo (CAMS)",
            creditsLine(openMeteoOnly, jakarta),
        )
    }
}
