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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
 * - BMKG is used inside coverage, at most once an hour ([BmkgThrottle]). A BMKG failure, a call
 *   slower than six seconds or an empty forecast hands over to Open-Meteo by passing `bmkg = null`
 *   to [merge], and BMKG is then left alone for 15 minutes. Meanwhile a cached BMKG forecast for
 *   the same kelurahan under six hours old is used rather than Open-Meteo.
 * - With no location fix (permission refused, no background access, or no fix yet) the last
 *   cached place is used; with neither, [LoadResult.NoLocation], saying whether permission is
 *   the reason.
 * - `forceRefresh = false` returns a snapshot under five minutes old without fetching, so the
 *   app and a widget refresh landing together don't fetch twice.
 * - "Now" for [merge] is Asia/Jakarta wall-clock time, matching BMKG and Open-Meteo.
 * - Everything runs on [dispatcher], so parsing, merging and the first kelurahan lookup stay off
 *   the main thread.
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
    private val hasLocationPermission: () -> Boolean,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WeatherRepository {

    private val throttle = BmkgThrottle(clock)

    /** While BMKG is failing, a cached forecast this young still beats Open-Meteo's. */
    private val fallback = BmkgThrottle(clock, BMKG_FALLBACK_MAX_AGE)
    private val mutex = Mutex()

    override suspend fun cached(): WeatherSnapshot? = withContext(dispatcher) { cache.snapshot() }

    override suspend fun load(forceRefresh: Boolean): LoadResult = withContext(dispatcher) {
        mutex.withLock {
            val previous = cache.snapshot()
            if (!forceRefresh && previous != null && isWithin(previous.fetchedAt, RECENT)) {
                return@withLock LoadResult.Fresh(previous)
            }

            val point = attempt { location.current() }
                ?: previous?.place?.let { GeoPoint(it.latitude, it.longitude) }
                ?: return@withLock LoadResult.NoLocation(permissionDenied = !hasLocationPermission())

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
    }

    private suspend fun fetch(point: GeoPoint): WeatherSnapshot = coroutineScope {
        val lat = point.latitude
        val lon = point.longitude
        val nearest = attempt { kelurahan.nearest(lat, lon) }

        val om = async { openMeteo.forecast(lat, lon) }
        val station = async { attempt { air.nearest(lat, lon) } }
        val name = async { attempt { areaNamer.name(lat, lon) } }
        val fromBmkg = async { nearest?.let { bmkgFor(it) } }

        val forecast = om.await()
        val bmkgOutcome = fromBmkg.await()
        val areaName = name.await()?.takeIf { it.isNotBlank() } ?: nearest?.name ?: FALLBACK_AREA_NAME
        val place = Place(areaName, lat, lon, nearest?.code)
        val fetchedAt = clock.instant()
        val now = LocalDateTime.ofInstant(fetchedAt, JAKARTA)

        val snapshot = merge(place, bmkgOutcome?.forecast, forecast, station.await(), now, fetchedAt)
        attempt { cache.save(snapshot, bmkgOutcome?.keep, bmkgOutcome?.failedAt) }
        snapshot
    }

    /**
     * The BMKG forecast for [nearest]: the cached one while under an hour old, otherwise a fresh
     * one. A failure, a timeout or an empty forecast starts a [BMKG_RETRY_AFTER] back-off, during
     * which BMKG isn't called and a cached forecast under [BMKG_FALLBACK_MAX_AGE] stands in.
     */
    private suspend fun bmkgFor(nearest: Kelurahan): BmkgOutcome {
        val previous = attempt { cache.bmkg() }
        val failedAt = attempt { cache.bmkgFailedAt() }
        throttle.reusable(previous, nearest.code)?.let { return BmkgOutcome(it, previous, failedAt) }

        if (failedAt != null && isWithin(failedAt, BMKG_RETRY_AFTER)) {
            return BmkgOutcome(fallback.reusable(previous, nearest.code), previous, failedAt)
        }

        val fresh = withTimeoutOrNull(BMKG_TIMEOUT_MS) { attempt { bmkg.forecast(nearest.code) } }
        if (fresh == null || fresh.steps.isEmpty()) {
            return BmkgOutcome(fallback.reusable(previous, nearest.code), previous, clock.instant())
        }
        return BmkgOutcome(fresh, CachedBmkg(nearest.code, fresh, clock.instant()), failedAt = null)
    }

    /** True when [since] is in the past by less than [limit]; a clock that went backwards fails. */
    private fun isWithin(since: Instant, limit: Duration): Boolean {
        val age = Duration.between(since, clock.instant())
        return !age.isNegative && age < limit
    }

    /** Runs [block], turning any failure except cancellation into null. */
    private inline fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** What one fetch got from BMKG. */
    private class BmkgOutcome(
        /** For this merge; null hands over to Open-Meteo. */
        val forecast: BmkgForecast?,
        /** The BMKG forecast to keep in the cache. */
        val keep: CachedBmkg?,
        /** When BMKG last failed, or null after a success. */
        val failedAt: Instant?,
    )

    private companion object {
        val JAKARTA: ZoneId = ZoneId.of("Asia/Jakarta")
        val RECENT: Duration = Duration.ofMinutes(5)
        val BMKG_RETRY_AFTER: Duration = Duration.ofMinutes(15)
        val BMKG_FALLBACK_MAX_AGE: Duration = Duration.ofHours(6)
        const val BMKG_TIMEOUT_MS = 6_000L
        const val FALLBACK_AREA_NAME = "Your location"
    }
}
