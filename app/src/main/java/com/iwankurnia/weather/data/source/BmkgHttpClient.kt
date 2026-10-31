package com.iwankurnia.weather.data.source

import com.iwankurnia.weather.data.BmkgClient

/** BMKG open-data forecast for one kelurahan (adm4 code). Throws on any network or parse failure. */
class BmkgHttpClient(private val fetch: TextFetcher = ::getText) : BmkgClient {
    override suspend fun forecast(adm4: String): BmkgForecast =
        BmkgParser.parse(fetch("$BASE_URL?adm4=$adm4"))

    private companion object {
        const val BASE_URL = "https://api.bmkg.go.id/publik/prakiraan-cuaca"
    }
}
