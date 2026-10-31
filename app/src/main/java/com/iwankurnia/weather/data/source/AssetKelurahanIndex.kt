package com.iwankurnia.weather.data.source

import android.content.Context
import com.iwankurnia.weather.data.KelurahanIndex
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Mean Earth radius used by [haversineKm]. */
private const val EARTH_RADIUS_KM = 6371.0088

/** Nearest-kelurahan lookup over the bundled list, by straight-line (haversine) distance. */
class AssetKelurahanIndex(val rows: List<Kelurahan>) : KelurahanIndex {
    override fun nearest(lat: Double, lon: Double, maxKm: Double): Kelurahan? {
        var best: Kelurahan? = null
        var bestKm = Double.MAX_VALUE
        for (row in rows) {
            val km = haversineKm(lat, lon, row.latitude, row.longitude)
            if (km < bestKm) {
                best = row
                bestKm = km
            }
        }
        return best?.takeIf { bestKm <= maxKm }
    }

    companion object {
        private const val ASSET = "kelurahan.csv"

        /** Reads `assets/kelurahan.csv`. Call off the main thread. */
        fun load(context: Context): AssetKelurahanIndex =
            fromCsv(context.assets.open(ASSET).bufferedReader().use { it.readText() })

        /**
         * Parses CSV with the header `code,kelurahan,kecamatan,kota,lat,lon`.
         *
         * Columns are found by header name, quoted fields may contain commas,
         * and rows with missing or non-numeric coordinates are skipped.
         */
        fun fromCsv(text: String): AssetKelurahanIndex {
            val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotBlank() }.toList()
            if (lines.isEmpty()) return AssetKelurahanIndex(emptyList())
            val header = splitCsvLine(lines.first().removePrefix("﻿")).map { it.trim().lowercase() }
            val code = header.indexOf("code")
            val name = header.indexOf("kelurahan")
            val kecamatan = header.indexOf("kecamatan")
            val lat = header.indexOf("lat")
            val lon = header.indexOf("lon")
            require(listOf(code, name, kecamatan, lat, lon).none { it < 0 }) { "Kelurahan CSV: bad header" }

            val rows = lines.drop(1).mapNotNull { line ->
                val cells = splitCsvLine(line).map { it.trim() }
                Kelurahan(
                    code = cells.getOrNull(code)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null,
                    name = cells.getOrNull(name) ?: return@mapNotNull null,
                    kecamatan = cells.getOrNull(kecamatan) ?: return@mapNotNull null,
                    latitude = cells.getOrNull(lat)?.toDoubleOrNull() ?: return@mapNotNull null,
                    longitude = cells.getOrNull(lon)?.toDoubleOrNull() ?: return@mapNotNull null,
                )
            }
            return AssetKelurahanIndex(rows)
        }

        /** Splits one CSV line, honouring double quotes and `""` escapes. */
        private fun splitCsvLine(line: String): List<String> {
            val cells = mutableListOf<String>()
            val cell = StringBuilder()
            var quoted = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                when {
                    quoted && c == '"' && line.getOrNull(i + 1) == '"' -> {
                        cell.append('"')
                        i++
                    }
                    c == '"' -> quoted = !quoted
                    c == ',' && !quoted -> {
                        cells += cell.toString()
                        cell.clear()
                    }
                    else -> cell.append(c)
                }
                i++
            }
            cells += cell.toString()
            return cells
        }
    }
}

/** Great-circle distance in kilometres between two points given in degrees. */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).let { it * it } +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(a)))
}
