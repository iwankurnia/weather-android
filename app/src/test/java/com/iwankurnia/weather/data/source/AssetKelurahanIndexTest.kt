package com.iwankurnia.weather.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetKelurahanIndexTest {
    private val csv = """
        code,kelurahan,kecamatan,kota,lat,lon
        31.71.03.1001,Kemayoran,Kemayoran,Kota Adm. Jakarta Pusat,-6.16472,106.84538
        31.71.03.1002,Kebon Kosong,Kemayoran,Kota Adm. Jakarta Pusat,-6.16050,106.85310
        31.71.06.1001,Gelora,Tanah Abang,Kota Adm. Jakarta Pusat,-6.21650,106.80150
        32.76.01.1001,"Depok, Pancoran Mas",Pancoran Mas,Kota Depok,-6.40020,106.81860
    """.trimIndent()

    private val index = AssetKelurahanIndex.fromCsv(csv)

    @Test
    fun fromCsv_readsEveryRow() {
        assertEquals(4, index.rows.size)
        val quoted = index.rows.last()
        assertEquals("Depok, Pancoran Mas", quoted.name)
        assertEquals("Pancoran Mas", quoted.kecamatan)
        assertEquals(-6.4002, quoted.latitude, 1e-9)
    }

    @Test
    fun fromCsv_skipsMalformedRows() {
        val messy = AssetKelurahanIndex.fromCsv(
            "code,kelurahan,kecamatan,kota,lat,lon\r\n" +
                "31.71.03.1001,Kemayoran,Kemayoran,Jakarta Pusat,-6.16472,106.84538\r\n" +
                "bad,row\r\n" +
                "31.71.03.1009,Broken,Kemayoran,Jakarta Pusat,north,east\r\n\r\n",
        )
        assertEquals(listOf("31.71.03.1001"), messy.rows.map { it.code })
    }

    @Test
    fun nearest_returnsClosestWithinRadius() {
        // About 100 m north of Kemayoran's centre.
        val found = index.nearest(-6.16472 + 0.0009, 106.84538)

        assertEquals("31.71.03.1001", found?.code)
        assertEquals("Kemayoran", found?.name)
    }

    @Test
    fun nearest_farPoint_returnsNull() {
        // Bandung.
        assertNull(index.nearest(-6.9175, 107.6191))
    }

    @Test
    fun nearest_respectsCustomRadius() {
        // Gelora is about 1.5 km from this point.
        assertNull(index.nearest(-6.2297, 106.7997, maxKm = 1.0))
        assertEquals("31.71.06.1001", index.nearest(-6.2297, 106.7997, maxKm = 2.0)?.code)
    }

    @Test
    fun haversine_matchesKnownDistance() {
        // Kemayoran to Bandung is roughly 120 km.
        val km = haversineKm(-6.16472, 106.84538, -6.9175, 107.6191)
        assertEquals(120.0, km, 5.0)
    }
}
