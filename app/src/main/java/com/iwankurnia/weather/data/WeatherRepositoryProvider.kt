package com.iwankurnia.weather.data

import android.content.Context

/**
 * Implemented by the Application so the widgets and the background worker can reach the
 * shared repository and cache without a DI framework.
 */
interface WeatherRepositoryProvider {
    val weatherRepository: WeatherRepository
    val snapshotCache: SnapshotCache
}

/** The app's provider, or null when the Application doesn't implement it (not wired yet). */
fun Context.weatherRepositoryProvider(): WeatherRepositoryProvider? =
    applicationContext as? WeatherRepositoryProvider
