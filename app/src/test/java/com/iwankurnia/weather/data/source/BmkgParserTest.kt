package com.iwankurnia.weather.data.source

import java.time.LocalDateTime
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BmkgParserTest {
    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun bmkg_parsesLocalDatetimeAsJakarta() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val forecast = BmkgParser.parse(fixture("bmkg_kemayoran.json"))

        assertEquals(17, forecast.steps.size)
        assertEquals(LocalDateTime.of(2026, 11, 1, 4, 0), forecast.steps.first().local)
        assertEquals(LocalDateTime.of(2026, 11, 1, 19, 0), forecast.steps[5].local)
        assertEquals(LocalDateTime.of(2026, 11, 3, 4, 0), forecast.steps.last().local)
        val times = forecast.steps.map { it.local }
        assertEquals(times.sorted(), times)
    }

    @Test
    fun bmkg_mapsFields() {
        val forecast = BmkgParser.parse(fixture("bmkg_kemayoran.json"))

        assertEquals("Kemayoran", forecast.kelurahan)
        assertEquals("Kemayoran", forecast.kecamatan)
        assertEquals(-6.1647214778, forecast.latitude, 1e-9)
        assertEquals(106.8453837867, forecast.longitude, 1e-9)

        val first = forecast.steps.first()
        assertEquals(26.0, first.temperatureC, 0.0)
        assertEquals(90, first.humidityPct)
        assertEquals(2, first.weatherCode)
        assertEquals(5.0, first.windKmh, 0.0)
        assertEquals(219, first.windFromDeg)
        assertEquals(0.0, first.rainMm, 0.0)

        val rainy = forecast.steps[5]
        assertEquals(61, rainy.weatherCode)
        assertEquals(5.5, rainy.windKmh, 1e-9)
        assertEquals(230, rainy.windFromDeg)
        assertEquals(1.1, rainy.rainMm, 1e-9)
    }

    @Test
    fun bmkg_parsesNumbersSentAsStrings() {
        val json = """
            {"lokasi":{"adm4":"31.71.03.1001","desa":"Kemayoran","kecamatan":"Kemayoran",
              "lat":"-6.1647","lon":"106.8454"},
             "data":[{"lokasi":{},"cuaca":[[
               {"local_datetime":"2026-11-01 07:00:00","t":"27","hu":"85","weather":"2",
                "ws":"3.4","wd_deg":"207","tp":"0.5","vs":null},
               {"local_datetime":"2026-11-01 04:00:00","t":26,"hu":90,"weather":2,
                "ws":5,"wd_deg":219,"tp":0,"vs":null}
             ]]}]}
        """.trimIndent()

        val forecast = BmkgParser.parse(json)

        assertEquals(-6.1647, forecast.latitude, 1e-9)
        assertEquals(2, forecast.steps.size)
        assertEquals(LocalDateTime.of(2026, 11, 1, 4, 0), forecast.steps[0].local)
        val second = forecast.steps[1]
        assertEquals(27.0, second.temperatureC, 0.0)
        assertEquals(85, second.humidityPct)
        assertEquals(3.4, second.windKmh, 1e-9)
        assertEquals(0.5, second.rainMm, 1e-9)
    }

    @Test
    fun bmkg_skipsIncompleteSteps() {
        val json = """
            {"lokasi":{"desa":"Kemayoran","kecamatan":"Kemayoran","lat":-6.16,"lon":106.84},
             "data":[{"cuaca":[[
               {"local_datetime":"2026-11-01 04:00:00","t":null,"hu":90,"weather":2,"ws":5,"wd_deg":219,"tp":0},
               {"local_datetime":"2026-11-01 07:00:00","t":27,"hu":85,"weather":2,"ws":3,"wd_deg":207,"tp":0}
             ]]}]}
        """.trimIndent()

        val forecast = BmkgParser.parse(json)

        assertEquals(1, forecast.steps.size)
        assertTrue(forecast.steps.single().temperatureC == 27.0)
    }

    @Test(expected = Exception::class)
    fun bmkg_malformedJson_throws() {
        BmkgParser.parse("<html>Service unavailable</html>")
    }
}
