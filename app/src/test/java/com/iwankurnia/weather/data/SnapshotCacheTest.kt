package com.iwankurnia.weather.data

import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.BmkgStep
import com.iwankurnia.weather.model.SampleData
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SnapshotCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val bmkg = CachedBmkg(
        adm4 = "31.71.03.1001",
        forecast = BmkgForecast(
            "Kemayoran", "Kemayoran", -6.1617, 106.8560,
            listOf(
                BmkgStep(LocalDateTime.of(2026, 11, 1, 13, 0), 33.0, 70, 1, 10.0, 315, 0.0),
                BmkgStep(LocalDateTime.of(2026, 11, 1, 16, 0), 31.0, 80, 61, 8.5, 270, 2.4),
            ),
        ),
        fetchedAt = Instant.parse("2026-11-01T05:00:00Z"),
    )

    @Test
    fun cache_emptyWhenNoFile() = runTest {
        val cache = SnapshotCache(File(tmp.root, "cache.json"))
        assertNull(cache.snapshot())
        assertNull(cache.bmkg())
    }

    @Test
    fun cache_roundTripsSnapshotAndBmkg() = runTest {
        val file = File(tmp.root, "cache.json")
        SnapshotCache(file).save(SampleData.dayPartlyCloudy, bmkg)
        SnapshotCache(file).save(SampleData.nightRain, bmkg)

        val reread = SnapshotCache(file)
        assertEquals(SampleData.nightRain, reread.snapshot())
        assertEquals(bmkg, reread.bmkg())
        assertFalse(File(tmp.root, "cache.json.tmp").exists())
    }

    @Test
    fun cache_roundTripsBmkgFailureTime() = runTest {
        val file = File(tmp.root, "cache.json")
        val failedAt = Instant.parse("2026-11-01T05:30:00Z")
        SnapshotCache(file).save(SampleData.dayPartlyCloudy, bmkg, bmkgFailedAt = failedAt)
        assertEquals(failedAt, SnapshotCache(file).bmkgFailedAt())

        SnapshotCache(file).save(SampleData.dayPartlyCloudy, bmkg)
        assertNull(SnapshotCache(file).bmkgFailedAt())
    }

    @Test
    fun cache_fileWithoutFailureTime_stillLoads() = runTest {
        // Rewrite the file as a build without `bmkgFailedAt` would have written it.
        val file = File(tmp.root, "cache.json")
        SnapshotCache(file).save(SampleData.nightRain, bmkg, bmkgFailedAt = Instant.parse("2026-11-01T05:30:00Z"))
        file.writeText(file.readText().replace(Regex(",\\s*\"bmkgFailedAt\"\\s*:\\s*\"[^\"]*\""), ""))
        assertFalse(file.readText().contains("bmkgFailedAt"))

        val cache = SnapshotCache(file)
        assertEquals(SampleData.nightRain, cache.snapshot())
        assertEquals(bmkg, cache.bmkg())
        assertNull(cache.bmkgFailedAt())
    }

    @Test
    fun cache_corruptFile_readsAsEmpty() = runTest {
        val file = File(tmp.root, "cache.json")
        file.writeText("{not json")
        assertNull(SnapshotCache(file).snapshot())
    }
}
