package com.iwankurnia.weather.data

import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.Kelurahan
import com.iwankurnia.weather.data.source.OpenMeteoForecast
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.WeatherSnapshot

/** Throws on any network or parse failure. */
interface BmkgClient {
    suspend fun forecast(adm4: String): BmkgForecast
}

/** Throws on any network or parse failure. */
interface OpenMeteoClient {
    suspend fun forecast(lat: Double, lon: Double): OpenMeteoForecast
}

/** Station-based air quality; null on any failure or when no token is configured. */
interface AirClient {
    suspend fun nearest(lat: Double, lon: Double): AirQuality?
}

interface KelurahanIndex {
    /** Nearest kelurahan within [maxKm], or null (outside coverage). */
    fun nearest(lat: Double, lon: Double, maxKm: Double = 15.0): Kelurahan?
}

data class GeoPoint(val latitude: Double, val longitude: Double)

interface LocationSource {
    /** Null when permission is missing or no fix is available. */
    suspend fun current(): GeoPoint?
}

interface AreaNamer {
    suspend fun name(lat: Double, lon: Double): String?
}

sealed interface LoadResult {
    data class Fresh(val snapshot: WeatherSnapshot) : LoadResult

    /** Last good snapshot when a refresh failed; the UI shows the offline banner. */
    data class Stale(val snapshot: WeatherSnapshot, val reason: String) : LoadResult

    data class NoLocation(val permissionDenied: Boolean) : LoadResult

    data class Failed(val message: String) : LoadResult
}

interface WeatherRepository {
    suspend fun load(forceRefresh: Boolean): LoadResult

    suspend fun cached(): WeatherSnapshot?
}
