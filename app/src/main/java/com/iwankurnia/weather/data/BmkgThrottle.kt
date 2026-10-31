package com.iwankurnia.weather.data

import com.iwankurnia.weather.data.source.BmkgForecast
import java.time.Clock
import java.time.Duration

/**
 * BMKG only updates twice a day, so it is called at most once an hour per kelurahan.
 * Returns the cached forecast while it is younger than [maxAge], otherwise null.
 */
class BmkgThrottle(
    private val clock: Clock,
    private val maxAge: Duration = Duration.ofMinutes(60),
) {
    fun reusable(cached: CachedBmkg?, adm4: String): BmkgForecast? {
        if (cached == null || cached.adm4 != adm4) return null
        val age = Duration.between(cached.fetchedAt, clock.instant())
        // A negative age means the clock went backwards; fetch again to be safe.
        return cached.forecast.takeIf { !age.isNegative && age < maxAge }
    }
}
