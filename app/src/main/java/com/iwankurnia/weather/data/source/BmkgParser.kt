package com.iwankurnia.weather.data.source

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonObject

/** Parses the BMKG `prakiraan-cuaca` response for one kelurahan. */
object BmkgParser {
    /** BMKG's `local_datetime`, Asia/Jakarta wall-clock time with no offset. */
    private val localFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /**
     * Flattens every 3-hour step of every day into time order.
     *
     * Numbers sent as strings are accepted, and steps missing a required value are skipped.
     * Throws when the response is not BMKG JSON or has no location.
     */
    fun parse(json: String): BmkgForecast {
        val root = requireNotNull(sourceJson.parseToJsonElement(json).asObject()) { "BMKG: not an object" }
        val data = root["data"].asArray().orEmpty().mapNotNull { it.asObject() }
        val lokasi = root["lokasi"].asObject()
            ?: data.firstNotNullOfOrNull { it["lokasi"].asObject() }
            ?: throw IllegalArgumentException("BMKG: no lokasi")

        val steps = data
            .flatMap { day -> day["cuaca"].asArray().orEmpty() }
            .flatMap { block -> block.asArray().orEmpty() }
            .mapNotNull { it.asObject()?.let(::parseStep) }
            .sortedBy { it.local }

        return BmkgForecast(
            kelurahan = lokasi["desa"].text().orEmpty(),
            kecamatan = lokasi["kecamatan"].text().orEmpty(),
            latitude = lokasi["lat"].number() ?: throw IllegalArgumentException("BMKG: no lat"),
            longitude = lokasi["lon"].number() ?: throw IllegalArgumentException("BMKG: no lon"),
            steps = steps,
        )
    }

    private fun parseStep(step: JsonObject): BmkgStep? {
        val local = step["local_datetime"].text()?.trim()?.let {
            catchingOrNull { LocalDateTime.parse(it, localFormat) }
        } ?: return null
        return BmkgStep(
            local = local,
            temperatureC = step["t"].number() ?: return null,
            humidityPct = step["hu"].integer() ?: return null,
            weatherCode = step["weather"].integer() ?: return null,
            windKmh = step["ws"].number() ?: return null,
            windFromDeg = step["wd_deg"].integer() ?: return null,
            rainMm = step["tp"].number() ?: return null,
        )
    }
}
