package com.iwankurnia.weather.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WaqiParserTest {
    @Test
    fun waqi_parsesAqiStationAttribution() {
        val air = WaqiParser.parse(fixture("waqi_geo_ok.json"))
        assertNotNull(air)
        air!!

        assertEquals(158, air.usAqi)
        assertEquals("Jakarta Central (US Consulate), Indonesia", air.stationName)
        assertEquals("US Embassy and Consulates Air Quality Monitor in Indonesia", air.attribution)
        assertFalse(air.modelled)
    }

    @Test
    fun waqi_errorStatus_returnsNull() {
        assertNull(WaqiParser.parse(fixture("waqi_geo_error.json")))
        assertNull(WaqiParser.parse("{\"status\":\"ok\",\"data\":{\"aqi\":\"-\"}}"))
        assertNull(WaqiParser.parse("{\"status\":\"ok\""))
        assertNull(WaqiParser.parse("<html>Bad gateway</html>"))
        assertNull(WaqiParser.parse(""))
    }

    @Test
    fun waqi_missingCityAndAttributions_stillParses() {
        val air = WaqiParser.parse("{\"status\":\"ok\",\"data\":{\"aqi\":\"42\"}}")
        assertNotNull(air)
        air!!

        assertEquals(42, air.usAqi)
        assertNull(air.stationName)
        assertNull(air.attribution)
    }
}
