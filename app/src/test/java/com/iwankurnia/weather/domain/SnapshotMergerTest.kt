package com.iwankurnia.weather.domain

import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.BmkgStep
import com.iwankurnia.weather.data.source.OmDay
import com.iwankurnia.weather.data.source.OmHour
import com.iwankurnia.weather.data.source.OpenMeteoForecast
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.DataSource
import com.iwankurnia.weather.model.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SnapshotMergerTest {

    private val today = LocalDate.of(2026, 11, 1)
    private val place = Place("Kemayoran", -6.16, 106.85, "31.71.03.1001")
    private val fetchedAt = Instant.parse("2026-11-01T04:30:00Z")
    private val at1130 = today.atTime(11, 30)

    private fun at(dayOffset: Long, hour: Int): LocalDateTime =
        today.plusDays(dayOffset).atTime(hour, 0)

    /**
     * BMKG steps every 3 hours from 01:00 today for 3 days (24 steps).
     * Temperatures cycle 26..33 through each day; [edit] overrides single steps.
     */
    private fun bmkg(edit: (BmkgStep) -> BmkgStep = { it }): BmkgForecast {
        val steps = (0 until 24).map { i ->
            edit(
                BmkgStep(
                    local = at(0, 1).plusHours(3L * i),
                    temperatureC = 26.0 + (i % 8),
                    humidityPct = 75,
                    weatherCode = 1,
                    windKmh = 10.0,
                    windFromDeg = 90,
                    rainMm = 0.0,
                ),
            )
        }
        return BmkgForecast("Kemayoran", "Kemayoran", -6.16, 106.85, steps)
    }

    /** Hourly Open-Meteo data from yesterday 00:00 to day 10 23:00, plus 11 daily rows from yesterday. */
    private fun om(
        modelledUsAqi: Int? = null,
        edit: (OmHour) -> OmHour = { it },
    ): OpenMeteoForecast {
        val first = at(-1, 0)
        val hours = (0 until 11 * 24).map { i ->
            val t = first.plusHours(i.toLong())
            edit(
                OmHour(
                    time = t,
                    temperatureC = 27.0,
                    apparentC = 30.0,
                    humidityPct = 70,
                    weatherCode = 2,
                    rainChancePct = t.hour * 2,
                    precipitationMm = 0.5,
                    visibilityM = 20_000.0,
                    uvIndex = t.hour / 2.0,
                    dewPointC = 24.0 + t.hour / 10.0,
                    windKmh = 8.0,
                    windFromDeg = 180,
                    gustKmh = 10.0 + t.hour,
                    isDay = t.hour in 6..17,
                ),
            )
        }
        val days = (0..10).map { i ->
            OmDay(
                date = today.plusDays(i - 1L),
                lowC = 20.0 + i,
                highC = 30.0 + i,
                weatherCode = 3,
                rainChancePct = 5 * i,
                sunrise = LocalTime.of(5, 31),
                sunset = LocalTime.of(17, 50),
            )
        }
        return OpenMeteoForecast(hours, days, modelledUsAqi)
    }

    @Test
    fun merge_bmkgPrimary() {
        val forecast = bmkg {
            when (it.local) {
                at(0, 10) -> it.copy(temperatureC = 30.0, humidityPct = 80, weatherCode = 61, windKmh = 12.0)
                at(0, 13) -> it.copy(temperatureC = 33.0, humidityPct = 60, weatherCode = 3, windKmh = 20.0)
                else -> it
            }
        }
        val s = SnapshotMerger.merge(place, forecast, om(), null, at1130, fetchedAt)

        assertEquals(DataSource.BMKG, s.source)
        assertEquals(place, s.place)
        assertEquals(fetchedAt, s.fetchedAt)
        assertEquals(31.5, s.now.temperatureC, 1e-9)
        assertEquals(70, s.now.humidityPct)
        assertEquals(feelsLikeC(31.5, 70), s.now.feelsLikeC, 1e-9)
        assertEquals(Condition.RAIN, s.now.condition)
        assertEquals(12.0, s.now.windKmh, 1e-9)
        assertTrue(s.now.isDay)

        assertEquals(8, s.steps.size)
        assertEquals(at(0, 10), s.steps.first().time)
        s.steps.zipWithNext().forEach { (a, b) -> assertEquals(a.time.plusHours(3), b.time) }
        assertEquals(30.0, s.steps[0].temperatureC, 1e-9)
        assertEquals(Condition.RAIN, s.steps[0].condition)
        assertEquals(Condition.CLOUDY, s.steps[1].condition)
        s.steps.forEach { assertEquals(it.time.hour * 2, it.rainChancePct) }

        assertEquals("Rain likely from around 10:00.", s.summary)
    }

    @Test
    fun merge_nowConditionFromNearestStep() {
        val forecast = bmkg {
            when (it.local) {
                at(0, 10) -> it.copy(temperatureC = 30.0, humidityPct = 80, weatherCode = 61)
                at(0, 13) -> it.copy(temperatureC = 33.0, humidityPct = 60, weatherCode = 3)
                else -> it
            }
        }
        val early = SnapshotMerger.merge(place, forecast, om(), null, today.atTime(11, 0), fetchedAt)
        val late = SnapshotMerger.merge(place, forecast, om(), null, today.atTime(12, 30), fetchedAt)

        assertEquals(Condition.RAIN, early.now.condition)
        assertEquals(Condition.CLOUDY, late.now.condition)
        assertEquals(32.5, late.now.temperatureC, 1e-9)
    }

    @Test
    fun merge_daysSplitBySource() {
        val forecast = bmkg {
            when (it.local) {
                at(0, 7) -> it.copy(weatherCode = 3)
                at(1, 13) -> it.copy(weatherCode = 95)
                at(2, 10) -> it.copy(weatherCode = 61)
                at(2, 16) -> it.copy(weatherCode = 5)
                at(2, 22) -> it.copy(weatherCode = 95) // night, ignored for the day's condition
                else -> it
            }
        }
        val s = SnapshotMerger.merge(place, forecast, om(), null, at1130, fetchedAt)

        assertEquals(10, s.days.size)
        assertEquals((0L..9L).map { today.plusDays(it) }, s.days.map { it.date })
        assertEquals(List(3) { DataSource.BMKG } + List(7) { DataSource.OPEN_METEO }, s.days.map { it.source })

        assertEquals(26.0, s.days[0].lowC, 1e-9)
        assertEquals(33.0, s.days[0].highC, 1e-9)
        assertEquals(26.0, s.todayLowC, 1e-9)
        assertEquals(33.0, s.todayHighC, 1e-9)

        assertEquals(Condition.CLOUDY, s.days[0].condition)
        assertEquals(Condition.THUNDERSTORM, s.days[1].condition)
        assertEquals(Condition.RAIN, s.days[2].condition)

        // Open-Meteo daily rows start yesterday, so today is index 1 there (5% per index).
        assertEquals(listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50), s.days.map { it.rainChancePct })

        val day4 = s.days[3]
        assertEquals(24.0, day4.lowC, 1e-9)
        assertEquals(34.0, day4.highC, 1e-9)
        assertEquals(Condition.CLOUDY, day4.condition)
    }

    @Test
    fun merge_details() {
        val forecast = bmkg { if (it.local.toLocalDate() == today.plusDays(1)) it.copy(rainMm = 0.4) else it }
        val weather = om { hour ->
            when (hour.time) {
                at(-1, 11), at(0, 12) -> hour.copy(precipitationMm = 100.0) // just outside the 24 hours
                else -> hour
            }
        }
        val d = SnapshotMerger.merge(place, forecast, weather, null, at1130, fetchedAt).details

        assertEquals(3.2, d.rainTomorrowMm!!, 1e-9)
        assertEquals(12.0, d.rainLast24hMm!!, 1e-9)
        assertEquals(5.5, d.uvIndex!!, 1e-9)
        assertEquals(25.1, d.dewPointC!!, 1e-9)
        assertEquals(21.0, d.gustKmh!!, 1e-9)
        assertEquals(LocalTime.of(5, 31), d.sunrise)
        assertEquals(LocalTime.of(17, 50), d.sunset)
    }

    @Test
    fun merge_fallbackOpenMeteo() {
        val weather = om { hour ->
            when (hour.time) {
                at(0, 11) -> hour.copy(apparentC = 41.0, temperatureC = 34.0, weatherCode = 61)
                at(0, 14) -> hour.copy(weatherCode = 3, visibilityM = 3000.0, precipitationMm = 0.0)
                else -> hour
            }
        }
        for (bmkg in listOf(null, bmkg().copy(steps = emptyList()))) {
            val s = SnapshotMerger.merge(place, bmkg, weather, null, at1130, fetchedAt)

            assertEquals(DataSource.OPEN_METEO, s.source)
            assertEquals(34.0, s.now.temperatureC, 1e-9)
            assertEquals(41.0, s.now.feelsLikeC, 1e-9)
            assertEquals(Condition.RAIN, s.now.condition)

            assertEquals(8, s.steps.size)
            assertEquals(at(0, 11), s.steps.first().time)
            s.steps.zipWithNext().forEach { (a, b) -> assertEquals(a.time.plusHours(3), b.time) }
            assertEquals(Condition.RAIN, s.steps[0].condition)
            assertEquals(Condition.HAZE, s.steps[1].condition)
            assertEquals(Condition.PARTLY_CLOUDY, s.steps[2].condition)
            s.steps.forEach { assertEquals(it.time.hour * 2, it.rainChancePct) }

            assertEquals(10, s.days.size)
            assertEquals(today, s.days.first().date)
            assertTrue(s.days.all { it.source == DataSource.OPEN_METEO })
            assertEquals(21.0, s.todayLowC, 1e-9)
            assertEquals(31.0, s.todayHighC, 1e-9)
            assertEquals("Rain likely from around 11:00.", s.summary)
        }
    }

    @Test
    fun merge_nowIsLastStep_noCrash() {
        val now = at(2, 23).plusMinutes(30)
        val s = SnapshotMerger.merge(place, bmkg(), om(), null, now, fetchedAt)

        assertEquals(DataSource.BMKG, s.source)
        assertEquals(33.0, s.now.temperatureC, 1e-9)
        assertEquals(1, s.steps.size)
        assertEquals(at(2, 22), s.steps.single().time)
        assertEquals(26.0, s.todayLowC, 1e-9)
        assertEquals(33.0, s.todayHighC, 1e-9)
        assertEquals(today.plusDays(2), s.days.first().date)
        assertEquals(DataSource.BMKG, s.days.first().source)
        assertTrue(s.days.drop(1).all { it.source == DataSource.OPEN_METEO })
    }

    @Test
    fun merge_nowBeforeFirstStep_usesFirstStep() {
        val s = SnapshotMerger.merge(place, bmkg(), om(), null, today.atTime(0, 30), fetchedAt)

        assertEquals(26.0, s.now.temperatureC, 1e-9)
        assertEquals(at(0, 1), s.steps.first().time)
        assertEquals(8, s.steps.size)
    }

    @Test
    fun merge_isDay_usesSunriseSunset() {
        val base = bmkg().steps.first()
        val forecast = bmkg().copy(
            steps = listOf(5, 6, 17, 18).map { base.copy(local = at(0, it)) },
        )
        val s = SnapshotMerger.merge(place, forecast, om(), null, at(0, 5), fetchedAt)

        assertEquals(listOf(false, true, true, false), s.steps.map { it.isDay })
        assertFalse(s.now.isDay)
    }

    @Test
    fun merge_airFallback() {
        val modelled = SnapshotMerger.merge(place, bmkg(), om(modelledUsAqi = 163), null, at1130, fetchedAt)
        assertEquals(AirQuality(163, null, "Open-Meteo (CAMS)", modelled = true), modelled.air)

        val station = AirQuality(87, "Jakarta Central", "Jakarta EPA", modelled = false)
        val real = SnapshotMerger.merge(place, bmkg(), om(modelledUsAqi = 163), station, at1130, fetchedAt)
        assertEquals(station, real.air)

        assertNull(SnapshotMerger.merge(place, bmkg(), om(), null, at1130, fetchedAt).air)
    }

    @Test
    fun conditionCodes_bmkgTable() {
        val table = mapOf(
            Condition.CLEAR to listOf(0),
            Condition.PARTLY_CLOUDY to listOf(1, 2),
            Condition.CLOUDY to listOf(3, 4, 999),
            Condition.HAZE to listOf(5, 10, 45),
            Condition.RAIN to listOf(60, 61, 63, 80),
            Condition.THUNDERSTORM to listOf(95, 97),
        )
        table.forEach { (condition, codes) ->
            codes.forEach { assertEquals("code $it", condition, bmkgToCondition(it)) }
        }
    }

    @Test
    fun conditionCodes_wmoTable() {
        val table = mapOf(
            Condition.CLEAR to listOf(0),
            Condition.PARTLY_CLOUDY to listOf(1, 2),
            Condition.CLOUDY to listOf(3, 71, 999),
            Condition.HAZE to listOf(45, 48),
            Condition.RAIN to (51..67) + (80..82),
            Condition.THUNDERSTORM to (95..99).toList(),
        )
        table.forEach { (condition, codes) ->
            codes.forEach { assertEquals("code $it", condition, wmoToCondition(it, 20_000.0, raining = false)) }
        }
        assertEquals(Condition.HAZE, wmoToCondition(3, 4000.0, raining = false))
        assertEquals(Condition.CLOUDY, wmoToCondition(3, 4000.0, raining = true))
        assertEquals(Condition.CLOUDY, wmoToCondition(3, null, raining = false))
    }
}
