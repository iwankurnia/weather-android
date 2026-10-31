package com.iwankurnia.weather.data

import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.BmkgStep
import com.iwankurnia.weather.data.source.Kelurahan
import com.iwankurnia.weather.data.source.OpenMeteoForecast
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.DataSource
import com.iwankurnia.weather.model.Place
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.model.WeatherSnapshot
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultWeatherRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val kemayoran = Kelurahan("31.71.03.1001", "Kemayoran", "Kemayoran", -6.1617, 106.8560)
    private val here = GeoPoint(-6.1620, 106.8562)
    private val bmkgForecast = BmkgForecast(
        "Kemayoran", "Kemayoran", -6.1617, 106.8560,
        listOf(BmkgStep(LocalDateTime.of(2026, 11, 1, 13, 0), 33.0, 70, 1, 10.0, 315, 0.0)),
    )
    private val omForecast = OpenMeteoForecast(emptyList(), emptyList(), modelledUsAqi = 163)
    private val station = AirQuality(156, "Jakarta Selatan", "Ministry of Environment", modelled = false)

    private lateinit var clock: MutableClock
    private lateinit var location: FakeLocation
    private lateinit var namer: FakeNamer
    private lateinit var index: FakeIndex
    private lateinit var bmkg: FakeBmkg
    private lateinit var openMeteo: FakeOpenMeteo
    private lateinit var air: FakeAir
    private lateinit var cache: SnapshotCache
    private lateinit var merge: RecordingMerge

    @Before
    fun setUp() {
        clock = MutableClock(Instant.parse("2026-11-01T05:40:00Z"))
        location = FakeLocation(here)
        namer = FakeNamer("Kemayoran")
        index = FakeIndex(kemayoran)
        bmkg = FakeBmkg(bmkgForecast)
        openMeteo = FakeOpenMeteo(omForecast)
        air = FakeAir(station)
        cache = SnapshotCache(File(tmp.root, "cache.json"))
        merge = RecordingMerge()
    }

    private fun repository() = DefaultWeatherRepository(
        location = location,
        areaNamer = namer,
        kelurahan = index,
        bmkg = bmkg,
        openMeteo = openMeteo,
        air = air,
        cache = cache,
        merge = merge::invoke,
        clock = clock,
    )

    @Test
    fun repository_bmkgInsideCoverage() = runTest {
        val result = repository().load(forceRefresh = true)

        assertEquals(listOf("31.71.03.1001"), bmkg.calls)
        assertTrue(result is LoadResult.Fresh)
        val snapshot = (result as LoadResult.Fresh).snapshot
        assertEquals(DataSource.BMKG, snapshot.source)
        assertEquals("Kemayoran", snapshot.place.areaName)
        assertEquals("31.71.03.1001", snapshot.place.kelurahanCode)
        assertSame(bmkgForecast, merge.last.bmkg)
        assertEquals(LocalDateTime.of(2026, 11, 1, 12, 40), merge.last.now)
        assertEquals(clock.instant(), merge.last.fetchedAt)
        assertEquals(snapshot, cache.snapshot())
    }

    @Test
    fun repository_outsideCoverage_usesOpenMeteo() = runTest {
        index.result = null
        location.point = GeoPoint(-6.9175, 107.6191)
        namer.result = "Bandung"

        val result = repository().load(forceRefresh = true)

        assertTrue(bmkg.calls.isEmpty())
        assertNull(merge.last.bmkg)
        assertTrue(result is LoadResult.Fresh)
        val snapshot = (result as LoadResult.Fresh).snapshot
        assertEquals(DataSource.OPEN_METEO, snapshot.source)
        assertEquals("Bandung", snapshot.place.areaName)
        assertNull(snapshot.place.kelurahanCode)
    }

    @Test
    fun repository_bmkgFails_fallsBack() = runTest {
        bmkg.failure = IOException("BMKG down")

        val result = repository().load(forceRefresh = true)

        assertEquals(1, bmkg.calls.size)
        assertNull(merge.last.bmkg)
        assertTrue(result is LoadResult.Fresh)
    }

    @Test
    fun repository_bmkgNoSteps_fallsBack() = runTest {
        bmkg.forecast = bmkgForecast.copy(steps = emptyList())

        repository().load(forceRefresh = true)

        assertNull(merge.last.bmkg)
    }

    @Test
    fun repository_waqiMissing_usesModelledAir() = runTest {
        air.result = null

        val result = repository().load(forceRefresh = true)

        assertNull(merge.last.air)
        assertSame(omForecast, merge.last.om)
        assertTrue(result is LoadResult.Fresh)
    }

    @Test
    fun repository_bmkgThrottled() = runTest {
        val repo = repository()
        repo.load(forceRefresh = true)
        clock.advance(Duration.ofMinutes(45))

        repo.load(forceRefresh = true)

        assertEquals(1, bmkg.calls.size)
        assertEquals(2, merge.calls.size)
        // Reused from the cache file, so an equal copy rather than the same instance.
        assertEquals(bmkgForecast, merge.last.bmkg)
        assertEquals(2, openMeteo.calls)
    }

    @Test
    fun repository_bmkgThrottleExpires_after60Minutes() = runTest {
        val repo = repository()
        repo.load(forceRefresh = true)
        clock.advance(Duration.ofMinutes(61))

        repo.load(forceRefresh = true)

        assertEquals(2, bmkg.calls.size)
    }

    @Test
    fun repository_bmkgThrottle_ignoresOtherKelurahan() = runTest {
        val repo = repository()
        repo.load(forceRefresh = true)
        index.result = kemayoran.copy(code = "31.71.03.1002")
        clock.advance(Duration.ofMinutes(10))

        repo.load(forceRefresh = true)

        assertEquals(listOf("31.71.03.1001", "31.71.03.1002"), bmkg.calls)
    }

    @Test
    fun repository_offlineWithCache_returnsStale() = runTest {
        val repo = repository()
        val first = (repo.load(forceRefresh = true) as LoadResult.Fresh).snapshot
        openMeteo.failure = IOException("offline")
        clock.advance(Duration.ofMinutes(30))

        val result = repo.load(forceRefresh = true)

        assertTrue(result is LoadResult.Stale)
        assertEquals(first, (result as LoadResult.Stale).snapshot)
    }

    @Test
    fun repository_firstFetchFails_returnsError() = runTest {
        openMeteo.failure = IOException("offline")

        val result = repository().load(forceRefresh = true)

        assertTrue(result is LoadResult.Failed)
        assertNull(cache.snapshot())
    }

    @Test
    fun repository_mergeThrows_returnsError() = runTest {
        merge.failure = IllegalStateException("bad data")

        val result = repository().load(forceRefresh = true)

        assertTrue(result is LoadResult.Failed)
    }

    @Test
    fun repository_noPermission_returnsNoLocation() = runTest {
        location.point = null

        val result = repository().load(forceRefresh = true)

        assertEquals(LoadResult.NoLocation(permissionDenied = true), result)
        assertEquals(0, openMeteo.calls)
    }

    @Test
    fun repository_noLocationWithCache_usesCachedPlace() = runTest {
        val repo = repository()
        repo.load(forceRefresh = true)
        location.point = null
        location.failure = SecurityException("background location refused")
        clock.advance(Duration.ofMinutes(30))

        val result = repo.load(forceRefresh = true)

        assertTrue(result is LoadResult.Fresh)
        assertEquals(here.latitude, merge.last.place.latitude, 0.0)
        assertEquals(here.longitude, merge.last.place.longitude, 0.0)
    }

    @Test
    fun repository_areaName_fallsBackToKelurahanThenGeneric() = runTest {
        namer.result = null
        repository().load(forceRefresh = true)
        assertEquals("Kemayoran", merge.last.place.areaName)

        index.result = null
        repository().load(forceRefresh = true)
        assertEquals("Your location", merge.last.place.areaName)
    }

    @Test
    fun repository_notForced_reusesRecentSnapshot() = runTest {
        val repo = repository()
        repo.load(forceRefresh = true)
        clock.advance(Duration.ofMinutes(2))

        val result = repo.load(forceRefresh = false)

        assertTrue(result is LoadResult.Fresh)
        assertEquals(1, openMeteo.calls)
    }

    @Test
    fun repository_cached_returnsLastSnapshot() = runTest {
        val repo = repository()
        assertNull(repo.cached())
        val snapshot = (repo.load(forceRefresh = true) as LoadResult.Fresh).snapshot

        assertEquals(snapshot, repo.cached())
    }

    // --- fakes ---

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        fun advance(by: Duration) {
            now = now.plus(by)
        }
    }

    private class FakeLocation(var point: GeoPoint?) : LocationSource {
        var failure: Exception? = null
        override suspend fun current(): GeoPoint? {
            failure?.let { throw it }
            return point
        }
    }

    private class FakeNamer(var result: String?) : AreaNamer {
        override suspend fun name(lat: Double, lon: Double): String? = result
    }

    private class FakeIndex(var result: Kelurahan?) : KelurahanIndex {
        override fun nearest(lat: Double, lon: Double, maxKm: Double): Kelurahan? = result
    }

    private class FakeBmkg(var forecast: BmkgForecast) : BmkgClient {
        val calls = mutableListOf<String>()
        var failure: Exception? = null
        override suspend fun forecast(adm4: String): BmkgForecast {
            calls += adm4
            failure?.let { throw it }
            return forecast
        }
    }

    private class FakeOpenMeteo(var forecast: OpenMeteoForecast) : OpenMeteoClient {
        var calls = 0
        var failure: Exception? = null
        override suspend fun forecast(lat: Double, lon: Double): OpenMeteoForecast {
            calls++
            failure?.let { throw it }
            return forecast
        }
    }

    private class FakeAir(var result: AirQuality?) : AirClient {
        override suspend fun nearest(lat: Double, lon: Double): AirQuality? = result
    }

    private data class MergeCall(
        val place: Place,
        val bmkg: BmkgForecast?,
        val om: OpenMeteoForecast,
        val air: AirQuality?,
        val now: LocalDateTime,
        val fetchedAt: Instant,
    )

    /** Stands in for SnapshotMerger: records its inputs and builds a snapshot from the sample. */
    private class RecordingMerge {
        val calls = mutableListOf<MergeCall>()
        val last get() = calls.last()
        var failure: Exception? = null

        fun invoke(
            place: Place,
            bmkg: BmkgForecast?,
            om: OpenMeteoForecast,
            air: AirQuality?,
            now: LocalDateTime,
            fetchedAt: Instant,
        ): WeatherSnapshot {
            calls += MergeCall(place, bmkg, om, air, now, fetchedAt)
            failure?.let { throw it }
            return SampleData.dayPartlyCloudy.copy(
                place = place,
                source = if (bmkg != null) DataSource.BMKG else DataSource.OPEN_METEO,
                air = air,
                fetchedAt = fetchedAt,
            )
        }
    }
}
