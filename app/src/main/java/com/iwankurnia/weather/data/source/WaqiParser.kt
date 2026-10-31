package com.iwankurnia.weather.data.source

import com.iwankurnia.weather.model.AirQuality

/** Parses the WAQI `/feed/geo:` response for the nearest monitoring station. */
object WaqiParser {
    /**
     * Returns the station's US AQI, name and first attribution (the originating agency).
     *
     * Null for an error status, a missing reading (WAQI sends "-") or malformed JSON; never throws.
     */
    fun parse(json: String): AirQuality? = catchingOrNull {
        val root = sourceJson.parseToJsonElement(json).asObject() ?: return@catchingOrNull null
        if (root["status"].text() != "ok") return@catchingOrNull null
        val data = root["data"].asObject() ?: return@catchingOrNull null
        val aqi = data["aqi"].integer()?.takeIf { it >= 0 } ?: return@catchingOrNull null
        AirQuality(
            usAqi = aqi,
            stationName = data["city"].asObject()?.get("name").text()?.takeIf { it.isNotBlank() },
            attribution = data["attributions"].asArray()
                ?.firstNotNullOfOrNull { it.asObject()?.get("name").text()?.takeIf(String::isNotBlank) },
            modelled = false,
        )
    }
}
