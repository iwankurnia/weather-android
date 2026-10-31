package com.iwankurnia.weather

import android.app.Application
import com.iwankurnia.weather.data.SnapshotCache
import com.iwankurnia.weather.data.WeatherRepository
import com.iwankurnia.weather.data.WeatherRepositoryProvider
import com.iwankurnia.weather.di.AppGraph

/** Owns the app graph; widgets and the refresh worker reach the repository through [WeatherRepositoryProvider]. */
class WeatherApplication : Application(), WeatherRepositoryProvider {
    val graph: AppGraph by lazy { AppGraph(this) }

    override val weatherRepository: WeatherRepository get() = graph.weatherRepository
    override val snapshotCache: SnapshotCache get() = graph.snapshotCache
}
