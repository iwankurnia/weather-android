package com.iwankurnia.weather.data.source

import com.iwankurnia.weather.BuildConfig
import com.iwankurnia.weather.data.AirClient
import com.iwankurnia.weather.model.AirQuality
import java.net.URLEncoder

/**
 * WAQI reading from the monitoring station nearest to a point.
 *
 * Null on any failure, and when no token is configured, in which case nothing is requested.
 */
class WaqiHttpClient(
    private val token: String = BuildConfig.WAQI_TOKEN,
    private val fetch: TextFetcher = ::getText,
) : AirClient {
    override suspend fun nearest(lat: Double, lon: Double): AirQuality? {
        if (token.isBlank()) return null
        val encodedToken = URLEncoder.encode(token.trim(), "UTF-8")
        val body = catchingOrNull { fetch("$BASE_URL/geo:${coord(lat)};${coord(lon)}/?token=$encodedToken") }
        return body?.let(WaqiParser::parse)
    }

    private companion object {
        const val BASE_URL = "https://api.waqi.info/feed"
    }
}
