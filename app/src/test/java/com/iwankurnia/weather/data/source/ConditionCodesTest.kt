package com.iwankurnia.weather.data.source

import com.iwankurnia.weather.model.Condition
import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionCodesTest {
    @Test
    fun conditionCodes_bmkgTable() {
        val table = mapOf(
            0 to Condition.CLEAR,
            1 to Condition.PARTLY_CLOUDY,
            2 to Condition.PARTLY_CLOUDY,
            3 to Condition.CLOUDY,
            4 to Condition.CLOUDY,
            5 to Condition.HAZE,
            10 to Condition.HAZE,
            45 to Condition.HAZE,
            60 to Condition.RAIN,
            61 to Condition.RAIN,
            63 to Condition.RAIN,
            80 to Condition.RAIN,
            95 to Condition.THUNDERSTORM,
            97 to Condition.THUNDERSTORM,
            999 to Condition.CLOUDY,
        )
        table.forEach { (code, expected) -> assertEquals("BMKG $code", expected, bmkgCondition(code)) }
    }

    @Test
    fun conditionCodes_wmoTable() {
        val clearSky = 10_000.0
        val expectations = buildMap {
            put(0, Condition.CLEAR)
            put(1, Condition.PARTLY_CLOUDY)
            put(2, Condition.PARTLY_CLOUDY)
            put(3, Condition.CLOUDY)
            put(45, Condition.HAZE)
            put(48, Condition.HAZE)
            (51..67).forEach { put(it, Condition.RAIN) }
            (80..82).forEach { put(it, Condition.RAIN) }
            (95..99).forEach { put(it, Condition.THUNDERSTORM) }
            put(71, Condition.CLOUDY)
            put(999, Condition.CLOUDY)
        }
        expectations.forEach { (code, expected) ->
            assertEquals("WMO $code", expected, wmoCondition(code, clearSky, raining = false))
        }
    }

    @Test
    fun conditionCodes_wmoLowVisibilityWithoutRain_isHaze() {
        assertEquals(Condition.HAZE, wmoCondition(3, 4000.0, raining = false))
        assertEquals(Condition.HAZE, wmoCondition(0, 4999.0, raining = false))
    }

    @Test
    fun conditionCodes_wmoLowVisibilityWhileRaining_keepsCode() {
        assertEquals(Condition.CLOUDY, wmoCondition(3, 4000.0, raining = true))
        assertEquals(Condition.RAIN, wmoCondition(61, 2000.0, raining = false))
        assertEquals(Condition.CLOUDY, wmoCondition(3, 5000.0, raining = false))
        assertEquals(Condition.CLOUDY, wmoCondition(3, null, raining = false))
    }
}
