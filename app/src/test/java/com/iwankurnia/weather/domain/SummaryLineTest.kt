package com.iwankurnia.weather.domain

import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.ForecastStep
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SummaryLineTest {

    private val start = LocalDateTime.of(2026, 11, 1, 10, 0)
    private val at1450 = LocalDateTime.of(2026, 11, 1, 14, 50)

    /** Eight 3-hourly steps from 10:00; [conditions] and [chances] override by index. */
    private fun steps(
        conditions: Map<Int, Condition> = emptyMap(),
        chances: Map<Int, Int> = emptyMap(),
    ): List<ForecastStep> = (0 until 8).map { i ->
        ForecastStep(
            time = start.plusHours(3L * i),
            temperatureC = 30.0,
            condition = conditions[i] ?: Condition.PARTLY_CLOUDY,
            isDay = true,
            rainChancePct = chances[i] ?: 10,
        )
    }

    private fun feels(vararg values: Double): List<Pair<LocalDateTime, Double>> =
        (0 until 8).map { i -> start.plusHours(3L * i) to (values.getOrNull(i) ?: 30.0) }

    @Test
    fun summary_rainAndHeat() {
        val rainAt16 = steps(conditions = mapOf(2 to Condition.RAIN))
        assertEquals(
            "Rain likely from around 16:00. Very hot until then.",
            summaryLine(rainAt16, feels(40.0, 39.0, 36.0), start),
        )
        assertEquals("Rain likely from around 16:00.", summaryLine(rainAt16, feels(30.0, 31.0, 32.0), start))
        assertEquals("Very hot until 16:00.", summaryLine(steps(), feels(40.0, 39.0, 37.0), start))
        assertEquals("Dry and warm for the next few hours.", summaryLine(steps(), feels(30.0, 31.0), start))
    }

    @Test
    fun summary_hotForWholeWindow() {
        // 10:00–19:00 are the 12-hour window; 22:00 is outside it.
        assertEquals(
            "Very hot for the next few hours.",
            summaryLine(steps(), feels(40.0, 41.0, 39.0, 38.0, 30.0), start),
        )
    }

    @Test
    fun summary_rainChanceThreshold() {
        assertEquals(
            "Rain likely from around 13:00.",
            summaryLine(steps(chances = mapOf(1 to 50)), feels(), start),
        )
        assertEquals(
            "Dry and warm for the next few hours.",
            summaryLine(steps(chances = mapOf(1 to 49)), feels(), start),
        )
    }

    @Test
    fun summary_thunderstormCountsAsRain() {
        assertEquals(
            "Rain likely from around 19:00.",
            summaryLine(steps(conditions = mapOf(3 to Condition.THUNDERSTORM)), feels(), start),
        )
    }

    @Test
    fun summary_ignoresStepsBeyondTwelveHours() {
        val rainAt22 = steps(conditions = mapOf(4 to Condition.RAIN), chances = mapOf(5 to 90))
        assertEquals("Dry and warm for the next few hours.", summaryLine(rainAt22, feels(), start))
    }

    @Test
    fun summary_heatEndsBeforeRain_mentionsRainOnly() {
        // Hot only at 10:00, rain at 19:00: "Very hot until then" would be wrong.
        val rainAt19 = steps(conditions = mapOf(3 to Condition.RAIN))
        assertEquals("Rain likely from around 19:00.", summaryLine(rainAt19, feels(40.0, 36.0), start))
    }

    @Test
    fun summary_currentStep_countsAsNow() {
        // At 14:50 the 13:00 step is still current.
        val rainAt13 = steps(conditions = mapOf(1 to Condition.RAIN))
        assertEquals("Rain likely now.", summaryLine(rainAt13, feels(), at1450))
        assertEquals("Rain likely now.", summaryLine(rainAt13, feels(40.0, 40.0), at1450))
    }

    @Test
    fun summary_ignoresStepsAlreadyOver() {
        // At 14:50 the 10:00 step ended at 13:00.
        val rainAt10 = steps(conditions = mapOf(0 to Condition.RAIN))
        assertEquals("Dry and warm for the next few hours.", summaryLine(rainAt10, feels(), at1450))
    }

    @Test
    fun summary_looksTwelveHoursAheadOfNow() {
        // From 13:00 the window would end at 01:00; from 14:50 it runs to 02:50 and takes in 01:00.
        val rainAt01 = steps(conditions = mapOf(5 to Condition.RAIN)).drop(1)
        assertEquals("Rain likely from around 01:00.", summaryLine(rainAt01, feels(), at1450))
    }

    @Test
    fun summary_emptySteps_isPlain() {
        assertEquals("Dry and warm for the next few hours.", summaryLine(emptyList(), emptyList(), start))
    }
}
