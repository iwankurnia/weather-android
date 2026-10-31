package com.iwankurnia.weather.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatIndexTest {

    @Test
    fun heatIndex_noaaReference() {
        // 89.6 °F at 70% → about 105 °F on the NWS chart.
        assertEquals(40.6, feelsLikeC(32.0, 70), 0.5)
        // The plan quoted ~43.0 here, but the NWS regression gives 111.8 °F (44.3 °C),
        // which matches the NWS chart (92 °F/70% → 112 °F). The expected value follows NWS.
        assertEquals(44.3, feelsLikeC(33.0, 72), 0.3)
    }

    @Test
    fun heatIndex_mildUsesSimpleFormula() {
        // Below 80 °F NWS uses Steadman's simple formula: 77 °F at 90% → 78.6 °F (25.9 °C).
        // The plan expected the bare temperature (25.0); NWS gives slightly more.
        assertEquals(25.9, feelsLikeC(25.0, 90), 0.1)
    }

    @Test
    fun heatIndex_humidityAdjustments() {
        // High-humidity adjustment (RH > 85%, 80–87 °F): 28 °C at 90% is 33.7 unadjusted, 34.0 adjusted.
        assertEquals(34.0, feelsLikeC(28.0, 90), 0.1)
        // Low-humidity adjustment (RH < 13%, 80–112 °F) pulls it below the air temperature,
        // so the air temperature is returned.
        assertEquals(30.0, feelsLikeC(30.0, 10), 0.0)
    }

    @Test
    fun heatIndex_neverBelowTemperature() {
        for (t in 15..45) {
            for (rh in 0..100 step 5) {
                val tc = t.toDouble()
                assertTrue("$tc °C, $rh%", feelsLikeC(tc, rh) >= tc)
            }
        }
    }
}
