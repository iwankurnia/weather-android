package com.iwankurnia.weather.di

import android.content.Context
import com.iwankurnia.weather.data.DefaultWeatherRepository
import com.iwankurnia.weather.data.KelurahanIndex
import com.iwankurnia.weather.data.SnapshotCache
import com.iwankurnia.weather.data.WeatherRepository
import com.iwankurnia.weather.data.source.AssetKelurahanIndex
import com.iwankurnia.weather.data.source.BmkgHttpClient
import com.iwankurnia.weather.data.source.Kelurahan
import com.iwankurnia.weather.data.source.OpenMeteoHttpClient
import com.iwankurnia.weather.data.source.WaqiHttpClient
import com.iwankurnia.weather.data.source.location.FusedLocationSource
import com.iwankurnia.weather.data.source.location.GeocoderAreaNamer
import com.iwankurnia.weather.domain.SnapshotMerger
import java.time.Clock

/** Hand-wired object graph: one repository and one cache for the app, the widgets and the worker. */
class AppGraph(context: Context) {
    private val app = context.applicationContext

    val snapshotCache: SnapshotCache = SnapshotCache.inFilesDir(app)

    val weatherRepository: WeatherRepository = DefaultWeatherRepository(
        location = FusedLocationSource(app),
        areaNamer = GeocoderAreaNamer(app),
        kelurahan = LazyKelurahanIndex(app),
        bmkg = BmkgHttpClient(),
        openMeteo = OpenMeteoHttpClient(),
        air = WaqiHttpClient(),
        cache = snapshotCache,
        merge = SnapshotMerger::merge,
        clock = Clock.systemDefaultZone(),
    )
}

/** Reads the bundled kelurahan list on first lookup, so app start-up doesn't pay for it. */
private class LazyKelurahanIndex(private val context: Context) : KelurahanIndex {
    private val index by lazy { AssetKelurahanIndex.load(context) }

    override fun nearest(lat: Double, lon: Double, maxKm: Double): Kelurahan? = index.nearest(lat, lon, maxKm)
}
