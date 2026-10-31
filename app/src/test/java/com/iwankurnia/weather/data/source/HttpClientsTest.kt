package com.iwankurnia.weather.data.source

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpClientsTest {
    @Test
    fun waqi_blankToken_returnsNullWithoutRequest() = runTest {
        val requested = mutableListOf<String>()
        val client = WaqiHttpClient(token = "  ") { url -> requested += url; fixture("waqi_geo_ok.json") }

        assertNull(client.nearest(-6.2297, 106.7997))
        assertTrue(requested.isEmpty())
    }

    @Test
    fun waqi_requestsGeoFeedWithToken() = runTest {
        val requested = mutableListOf<String>()
        val client = WaqiHttpClient(token = "abc") { url -> requested += url; fixture("waqi_geo_ok.json") }

        val air = client.nearest(-6.2297, 106.7997)

        assertEquals(158, air?.usAqi)
        assertEquals(listOf("https://api.waqi.info/feed/geo:-6.2297;106.7997/?token=abc"), requested)
    }

    @Test
    fun waqi_networkFailure_returnsNull() = runTest {
        val client = WaqiHttpClient(token = "abc") { throw java.io.IOException("offline") }

        assertNull(client.nearest(-6.2297, 106.7997))
    }

    @Test
    fun bmkg_requestsKelurahanCode() = runTest {
        val requested = mutableListOf<String>()
        val client = BmkgHttpClient { url -> requested += url; fixture("bmkg_kemayoran.json") }

        val forecast = client.forecast("31.71.03.1001")

        assertEquals(17, forecast.steps.size)
        assertEquals(listOf("https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=31.71.03.1001"), requested)
    }

    @Test
    fun openMeteo_airFailure_stillReturnsForecast() = runTest {
        val requested = mutableListOf<String>()
        val client = OpenMeteoHttpClient { url ->
            requested += url
            if (url.startsWith("https://air-quality-api.open-meteo.com/")) throw java.io.IOException("offline")
            fixture("openmeteo_forecast.json")
        }

        val forecast = client.forecast(-6.2297, 106.7997)

        assertEquals(10, forecast.days.size)
        assertNull(forecast.modelledUsAqi)
        val forecastUrl = requested.single { it.startsWith("https://api.open-meteo.com/v1/forecast?") }
        assertTrue(forecastUrl.contains("latitude=-6.2297&longitude=106.7997"))
        assertTrue(forecastUrl.contains("past_days=1"))
        assertTrue(forecastUrl.contains("forecast_days=10"))
        assertTrue(forecastUrl.contains("timezone=Asia%2FJakarta"))
        assertTrue(forecastUrl.contains("is_day"))
    }

    @Test
    fun openMeteo_readsModelledAqi() = runTest {
        val client = OpenMeteoHttpClient { url ->
            if (url.startsWith("https://air-quality-api.open-meteo.com/v1/air-quality?")) {
                assertTrue(url.contains("current=us_aqi"))
                fixture("openmeteo_air.json")
            } else {
                fixture("openmeteo_forecast.json")
            }
        }

        assertEquals(163, client.forecast(-6.2297, 106.7997).modelledUsAqi)
    }

    @Test(expected = java.io.IOException::class)
    fun openMeteo_forecastFailure_throws() = runTest {
        val client = OpenMeteoHttpClient { url ->
            if (url.startsWith("https://api.open-meteo.com/")) throw java.io.IOException("offline")
            fixture("openmeteo_air.json")
        }
        client.forecast(-6.2297, 106.7997)
    }
}
