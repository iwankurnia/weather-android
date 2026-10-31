package com.iwankurnia.weather.data

import android.content.Context
import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.BmkgStep
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.DataSource
import com.iwankurnia.weather.model.DayForecast
import com.iwankurnia.weather.model.Details
import com.iwankurnia.weather.model.ForecastStep
import com.iwankurnia.weather.model.Now
import com.iwankurnia.weather.model.Place
import com.iwankurnia.weather.model.WeatherSnapshot
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The raw BMKG forecast last fetched, kept so [BmkgThrottle] can reuse it. */
data class CachedBmkg(val adm4: String, val forecast: BmkgForecast, val fetchedAt: Instant)

/**
 * The last good [WeatherSnapshot] and BMKG forecast, as one JSON file.
 * Writes go to a temp file that is then renamed over the old one, so a reader (the widget)
 * never sees a half-written file. Any unreadable file reads as empty.
 */
class SnapshotCache(
    private val file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()

    suspend fun snapshot(): WeatherSnapshot? = read { it.snapshot?.toModel() }

    suspend fun bmkg(): CachedBmkg? = read { it.bmkg?.toModel() }

    /** Replaces both entries. Pass `bmkg = null` when this fetch used no BMKG data. */
    suspend fun save(snapshot: WeatherSnapshot, bmkg: CachedBmkg?) = withContext(io) {
        mutex.withLock {
            val text = json.encodeToString(CacheFile.serializer(), CacheFile(VERSION, snapshot.toDto(), bmkg?.toDto()))
            file.parentFile?.mkdirs()
            val temp = File(file.path + ".tmp")
            temp.writeText(text)
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    /** Decodes the file and maps it with [pick]; null when missing, corrupt or from another version. */
    private suspend fun <T> read(pick: (CacheFile) -> T?): T? = withContext(io) {
        mutex.withLock {
            try {
                if (!file.exists()) return@withLock null
                val cached = json.decodeFromString(CacheFile.serializer(), file.readText())
                if (cached.version == VERSION) pick(cached) else null
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        private const val VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        /** The app's cache file in `filesDir`. */
        fun inFilesDir(context: Context): SnapshotCache =
            SnapshotCache(File(context.filesDir, "weather-cache.json"))
    }
}

// --- @Serializable mirrors of the model, so the model itself stays free of serialisation ---

@Serializable
private data class CacheFile(val version: Int, val snapshot: SnapshotDto? = null, val bmkg: CachedBmkgDto? = null)

@Serializable
private data class SnapshotDto(
    val place: PlaceDto,
    val now: NowDto,
    val todayHighC: Double,
    val todayLowC: Double,
    val steps: List<StepDto>,
    val days: List<DayDto>,
    val details: DetailsDto,
    val air: AirDto?,
    val summary: String,
    val source: String,
    val fetchedAt: String,
)

@Serializable
private data class PlaceDto(val areaName: String, val latitude: Double, val longitude: Double, val kelurahanCode: String?)

@Serializable
private data class NowDto(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPct: Int,
    val condition: String,
    val windKmh: Double,
    val windFromDeg: Int,
    val isDay: Boolean,
)

@Serializable
private data class StepDto(
    val time: String,
    val temperatureC: Double,
    val condition: String,
    val isDay: Boolean,
    val rainChancePct: Int?,
)

@Serializable
private data class DayDto(
    val date: String,
    val lowC: Double,
    val highC: Double,
    val condition: String,
    val rainChancePct: Int?,
    val source: String,
)

@Serializable
private data class AirDto(val usAqi: Int, val stationName: String?, val attribution: String?, val modelled: Boolean)

@Serializable
private data class DetailsDto(
    val uvIndex: Double?,
    val dewPointC: Double?,
    val gustKmh: Double?,
    val rainLast24hMm: Double?,
    val rainTomorrowMm: Double?,
    val sunrise: String,
    val sunset: String,
)

@Serializable
private data class CachedBmkgDto(
    val adm4: String,
    val fetchedAt: String,
    val kelurahan: String,
    val kecamatan: String,
    val latitude: Double,
    val longitude: Double,
    val steps: List<BmkgStepDto>,
)

@Serializable
private data class BmkgStepDto(
    val local: String,
    val temperatureC: Double,
    val humidityPct: Int,
    val weatherCode: Int,
    val windKmh: Double,
    val windFromDeg: Int,
    val rainMm: Double,
)

private fun WeatherSnapshot.toDto() = SnapshotDto(
    place = PlaceDto(place.areaName, place.latitude, place.longitude, place.kelurahanCode),
    now = NowDto(
        now.temperatureC, now.feelsLikeC, now.humidityPct, now.condition.name,
        now.windKmh, now.windFromDeg, now.isDay,
    ),
    todayHighC = todayHighC,
    todayLowC = todayLowC,
    steps = steps.map { StepDto(it.time.toString(), it.temperatureC, it.condition.name, it.isDay, it.rainChancePct) },
    days = days.map {
        DayDto(it.date.toString(), it.lowC, it.highC, it.condition.name, it.rainChancePct, it.source.name)
    },
    details = DetailsDto(
        details.uvIndex, details.dewPointC, details.gustKmh, details.rainLast24hMm, details.rainTomorrowMm,
        details.sunrise.toString(), details.sunset.toString(),
    ),
    air = air?.let { AirDto(it.usAqi, it.stationName, it.attribution, it.modelled) },
    summary = summary,
    source = source.name,
    fetchedAt = fetchedAt.toString(),
)

private fun SnapshotDto.toModel() = WeatherSnapshot(
    place = Place(place.areaName, place.latitude, place.longitude, place.kelurahanCode),
    now = Now(
        now.temperatureC, now.feelsLikeC, now.humidityPct, Condition.valueOf(now.condition),
        now.windKmh, now.windFromDeg, now.isDay,
    ),
    todayHighC = todayHighC,
    todayLowC = todayLowC,
    steps = steps.map {
        ForecastStep(LocalDateTime.parse(it.time), it.temperatureC, Condition.valueOf(it.condition), it.isDay, it.rainChancePct)
    },
    days = days.map {
        DayForecast(
            LocalDate.parse(it.date), it.lowC, it.highC, Condition.valueOf(it.condition),
            it.rainChancePct, DataSource.valueOf(it.source),
        )
    },
    details = Details(
        details.uvIndex, details.dewPointC, details.gustKmh, details.rainLast24hMm, details.rainTomorrowMm,
        LocalTime.parse(details.sunrise), LocalTime.parse(details.sunset),
    ),
    air = air?.let { AirQuality(it.usAqi, it.stationName, it.attribution, it.modelled) },
    summary = summary,
    source = DataSource.valueOf(source),
    fetchedAt = Instant.parse(fetchedAt),
)

private fun CachedBmkg.toDto() = CachedBmkgDto(
    adm4 = adm4,
    fetchedAt = fetchedAt.toString(),
    kelurahan = forecast.kelurahan,
    kecamatan = forecast.kecamatan,
    latitude = forecast.latitude,
    longitude = forecast.longitude,
    steps = forecast.steps.map {
        BmkgStepDto(it.local.toString(), it.temperatureC, it.humidityPct, it.weatherCode, it.windKmh, it.windFromDeg, it.rainMm)
    },
)

private fun CachedBmkgDto.toModel() = CachedBmkg(
    adm4 = adm4,
    forecast = BmkgForecast(
        kelurahan, kecamatan, latitude, longitude,
        steps.map {
            BmkgStep(
                LocalDateTime.parse(it.local), it.temperatureC, it.humidityPct, it.weatherCode,
                it.windKmh, it.windFromDeg, it.rainMm,
            )
        },
    ),
    fetchedAt = Instant.parse(fetchedAt),
)
