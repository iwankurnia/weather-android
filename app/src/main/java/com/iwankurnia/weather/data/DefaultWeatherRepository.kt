package com.iwankurnia.weather.data

import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.Kelurahan
import com.iwankurnia.weather.data.source.OpenMeteoForecast
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.Place
import com.iwankurnia.weather.model.WeatherSnapshot
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Builds a snapshot from the sources; `SnapshotMerger::merge` in the app. */
typealias SnapshotMerge = (
    place: Place,
    bmkg: BmkgForecast?,
    om: OpenMeteoForecast,
    air: AirQuality?,
    now: LocalDateTime,
    fetchedAt: Instant,
) -> WeatherSnapshot

/**
 * Adds location, caching and fallbacks around the source clients.
 *
 * - Open-Meteo is required; when it fails the last snapshot comes back as [LoadResult.Stale],
 *   or [LoadResult.Failed] when there is none.
 * - BMKG is used inside coverage, at most once an hour ([BmkgThrottle]); any BMKG failure or an
 *   empty forecast hands over to Open-Meteo by passing `bmkg = null` to [merge].
 * - With no location fix (permission refused, or no background access) the last cached place
 *   is used; with neither, [LoadResult.NoLocation].
 * - `forceRefresh = false` returns a snapshot under five minutes old without fetching, so the
 *   app and a widget refresh landing together don't fetch twice.
 * - "Now" for [merge] is Asia/Jakarta wall-clock time, matching BMKG and Open-Meteo.
 */
class DefaultWeatherRepository(
    private val location: LocationSource,
    private val areaNamer: AreaNamer,
    private val kelurahan: KelurahanIndex,
    private val bmkg: BmkgClient,
    private val openMeteo: OpenMeteoClient,
    private val air: AirClient,
    private val cache: SnapshotCache,
    private val merge: SnapshotMerge,
    private val clock: Clock,
) : WeatherRepository {

    private val throttle = BmkgThrottle(clock)
    private val mutex = Mutex()

    override suspend fun cached(): WeatherSnapshot? = cache.snapshot()

    override suspend fun load(forceRefresh: Boolean): LoadResult = mutex.withLock {
        val previous = cache.snapshot()
        if (!forceRefresh && previous != null && isRecent(previous)) return@withLock LoadResult.Fresh(previous)

        val point = attempt { location.current() }
            ?: previous?.place?.let { GeoPoint(it.latitude, it.longitude) }
            ?: return@withLock LoadResult.NoLocation(permissionDenied = true)

        try {
            val snapshot = fetch(point)
            LoadResult.Fresh(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (previous != null) {
                LoadResult.Stale(previous, "Couldn't refresh the forecast")
            } else {
                LoadResult.Failed("Couldn't reach the weather service. Check your connection and try again.")
            }
        }
    }

    private suspend fun fetch(point: GeoPoint): WeatherSnapshot = coroutineScope {
        val lat = point.latitude
        val lon = point.longitude
        val nearest = attempt { kelurahan.nearest(lat, lon) }

        val om = async { openMeteo.forecast(lat, lon) }
        val station = async { attempt { air.nearest(lat, lon) } }
        val name = async { attempt { areaNamer.name(lat, lon) } }
        val bmkgEntry = async { nearest?.let { bmkgFor(it) } }

        val forecast = om.await()
        val entry = bmkgEntry.await()
        val areaName = name.await()?.takeIf { it.isNotBlank() } ?: nearest?.name ?: FALLBACK_AREA_NAME
        val place = Place(areaName, lat, lon, nearest?.code)
        val fetchedAt = clock.instant()
        val now = LocalDateTime.ofInstant(fetchedAt, JAKARTA)

        val snapshot = merge(place, entry?.forecast, forecast, station.await(), now, fetchedAt)
        attempt { cache.save(snapshot, entry) }
        snapshot
    }

    /** The throttled or freshly fetched BMKG forecast, or null when BMKG fails or has no steps. */
    private suspend fun bmkgFor(nearest: Kelurahan): CachedBmkg? {
        val previous = attempt { cache.bmkg() }
        throttle.reusable(previous, nearest.code)?.let { return previous }
        val fresh = attempt { bmkg.forecast(nearest.code) } ?: return null
        if (fresh.steps.isEmpty()) return null
        return CachedBmkg(nearest.code, fresh, clock.instant())
    }

    private fun isRecent(snapshot: WeatherSnapshot): Boolean {
        val age = Duration.between(snapshot.fetchedAt, clock.instant())
        return !age.isNegative && age < RECENT
    }

    /** Runs [block], turning any failure except cancellation into null. */
    private inline fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        val JAKARTA: ZoneId = ZoneId.of("Asia/Jakarta")
        val RECENT: Duration = Duration.ofMinutes(5)
        const val FALLBACK_AREA_NAME = "Your location"
    }
}
