package com.iwankurnia.weather.data.source

import com.iwankurnia.weather.data.OpenMeteoClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Open-Meteo forecast plus the CAMS modelled air quality, fetched in parallel.
 *
 * Throws when the forecast fails. A failed air-quality call only leaves `modelledUsAqi` null.
 */
class OpenMeteoHttpClient(private val fetch: TextFetcher = ::getText) : OpenMeteoClient {
    override suspend fun forecast(lat: Double, lon: Double): OpenMeteoForecast = coroutineScope {
        val location = "latitude=${coord(lat)}&longitude=${coord(lon)}"
        val air = async { catchingOrNull { fetch("$AIR_URL?$location&current=us_aqi&timezone=$TIMEZONE") } }
        val forecast = fetch("$FORECAST_URL?$location&hourly=$HOURLY&daily=$DAILY&$RANGE&timezone=$TIMEZONE")
        OpenMeteoParser.parse(forecast, air.await())
    }

    private companion object {
        const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
        const val AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val TIMEZONE = "Asia%2FJakarta"
        const val RANGE = "past_days=1&forecast_days=10"
        const val HOURLY = "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code," +
            "precipitation_probability,precipitation,visibility,uv_index,dew_point_2m,wind_speed_10m," +
            "wind_direction_10m,wind_gusts_10m,is_day"
        const val DAILY = "temperature_2m_min,temperature_2m_max,weather_code,precipitation_probability_max," +
            "sunrise,sunset"
    }
}
