package com.iwankurnia.weather.domain

import com.iwankurnia.weather.data.source.BmkgForecast
import com.iwankurnia.weather.data.source.BmkgStep
import com.iwankurnia.weather.data.source.OmDay
import com.iwankurnia.weather.data.source.OmHour
import com.iwankurnia.weather.data.source.OpenMeteoForecast
import com.iwankurnia.weather.model.AirQuality
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.DataSource
import com.iwankurnia.weather.model.DayForecast
import com.iwankurnia.weather.model.Details
import com.iwankurnia.weather.model.ForecastStep
import com.iwankurnia.weather.model.Now
import com.iwankurnia.weather.model.Place
import com.iwankurnia.weather.model.WeatherSnapshot
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns BMKG and Open-Meteo source data into the one [WeatherSnapshot] the UI and widgets read.
 *
 * BMKG is primary for "now", the 3-hour steps and days 1–3; Open-Meteo fills chance of rain,
 * UV, dew point, gusts, sunrise and sunset, rain so far and days 4–10. When [merge] gets no
 * BMKG steps, Open-Meteo supplies everything.
 */
object SnapshotMerger {

    private const val STEP_HOURS = 3L
    private const val STEPS_SPAN_HOURS = 24L
    private const val FORECAST_DAYS = 10L
    private const val BMKG_DAYS = 3L
    private val DAYTIME = LocalTime.of(6, 0)..LocalTime.of(18, 0)

    /** Severity used to pick a day's condition, mildest first. */
    private val SEVERITY = listOf(
        Condition.CLEAR,
        Condition.PARTLY_CLOUDY,
        Condition.CLOUDY,
        Condition.HAZE,
        Condition.RAIN,
        Condition.THUNDERSTORM,
    )

    /**
     * Merges the sources into a snapshot for [now] (Asia/Jakarta wall-clock time).
     *
     * [place] is used as given. A null or empty [bmkg] means the Open-Meteo fallback.
     *
     * @throws IllegalArgumentException when there are neither BMKG steps nor Open-Meteo hours.
     */
    fun merge(
        place: Place,
        bmkg: BmkgForecast?,
        om: OpenMeteoForecast,
        air: AirQuality?,
        now: LocalDateTime,
        fetchedAt: Instant,
    ): WeatherSnapshot {
        val bmkgSteps = bmkg?.steps?.sortedBy { it.local }.orEmpty()
        val ctx = Context(om, now)
        val core = if (bmkgSteps.isNotEmpty()) fromBmkg(bmkgSteps, ctx) else fromOpenMeteo(ctx)

        // The first pill uses the current "feels like", so the line agrees with the hero.
        val feelsLike = core.stepFeelsLike.mapIndexed { i, pair ->
            if (i == 0) pair.first to core.now.feelsLikeC else pair
        }

        return WeatherSnapshot(
            place = place,
            now = core.now,
            todayHighC = core.todayHighC,
            todayLowC = core.todayLowC,
            steps = core.steps,
            days = core.days,
            details = details(ctx, core.rainTomorrowMm),
            air = air ?: om.modelledUsAqi?.let { AirQuality(it, null, "Open-Meteo (CAMS)", modelled = true) },
            summary = summaryLine(core.steps, feelsLike),
            source = core.source,
            fetchedAt = fetchedAt,
        )
    }

    /** Inputs shared by both paths. */
    private class Context(val om: OpenMeteoForecast, val now: LocalDateTime) {
        val today: LocalDate = now.toLocalDate()
        val omDays: Map<LocalDate, OmDay> = om.days.associateBy { it.date }
        val omHours: Map<LocalDateTime, OmHour> = om.hours.associateBy { it.time.truncatedTo(ChronoUnit.HOURS) }

        /** Day runs from that date's sunrise to sunset; without a daily row, Open-Meteo's own flag. */
        fun isDay(time: LocalDateTime): Boolean {
            val day = omDays[time.toLocalDate()]
            if (day != null) return time.toLocalTime().let { it >= day.sunrise && it < day.sunset }
            return omHours[time.truncatedTo(ChronoUnit.HOURS)]?.isDay ?: (time.hour in 6..17)
        }

        fun rainChanceAt(time: LocalDateTime): Int? = omHours[time.truncatedTo(ChronoUnit.HOURS)]?.rainChancePct

        fun omDayForecast(date: LocalDate): DayForecast? = omDays[date]?.let {
            DayForecast(
                date = it.date,
                lowC = it.lowC,
                highC = it.highC,
                condition = wmoToCondition(it.weatherCode, null, raining = false),
                rainChancePct = it.rainChancePct,
                source = DataSource.OPEN_METEO,
            )
        }
    }

    /** What differs between the BMKG and Open-Meteo paths. */
    private class Core(
        val now: Now,
        val todayHighC: Double,
        val todayLowC: Double,
        val steps: List<ForecastStep>,
        val stepFeelsLike: List<Pair<LocalDateTime, Double>>,
        val days: List<DayForecast>,
        val rainTomorrowMm: Double?,
        val source: DataSource,
    )

    private fun fromBmkg(steps: List<BmkgStep>, ctx: Context): Core {
        val now = ctx.now
        val startIndex = steps.indexOfLast { it.local <= now }.coerceAtLeast(0)
        val prev = steps[startIndex]
        val next = steps.getOrNull(startIndex + 1)?.takeIf { prev.local <= now }

        // Interpolate between the neighbouring steps; before the first or after the last, hold it.
        val fraction = if (next == null) {
            0.0
        } else {
            Duration.between(prev.local, now).toMillis().toDouble() /
                Duration.between(prev.local, next.local).toMillis()
        }
        val temperature = lerp(prev.temperatureC, next?.temperatureC, fraction)
        val humidity = lerp(prev.humidityPct.toDouble(), next?.humidityPct?.toDouble(), fraction).roundToInt()
        val nearest = if (next != null && fraction > 0.5) next else prev

        val nowValue = Now(
            temperatureC = temperature,
            feelsLikeC = feelsLikeC(temperature, humidity),
            humidityPct = humidity,
            condition = bmkgToCondition(nearest.weatherCode),
            windKmh = nearest.windKmh,
            windFromDeg = nearest.windFromDeg,
            isDay = ctx.isDay(now),
        )

        val windowEnd = prev.local.plusHours(STEPS_SPAN_HOURS)
        val pills = steps.drop(startIndex).filter { it.local < windowEnd }
        val forecastSteps = pills.map {
            ForecastStep(
                time = it.local,
                temperatureC = it.temperatureC,
                condition = bmkgToCondition(it.weatherCode),
                isDay = ctx.isDay(it.local),
                rainChancePct = ctx.rainChanceAt(it.local),
            )
        }

        val byDate = steps.groupBy { it.local.toLocalDate() }
        val todaySteps = byDate[ctx.today]
        val omToday = ctx.omDays[ctx.today]

        val days = (0 until FORECAST_DAYS).mapNotNull { offset ->
            val date = ctx.today.plusDays(offset)
            val daySteps = byDate[date]
            if (offset < BMKG_DAYS && daySteps != null) bmkgDay(date, daySteps, ctx) else ctx.omDayForecast(date)
        }

        return Core(
            now = nowValue,
            todayHighC = todaySteps?.maxOf { it.temperatureC } ?: omToday?.highC ?: temperature,
            todayLowC = todaySteps?.minOf { it.temperatureC } ?: omToday?.lowC ?: temperature,
            steps = forecastSteps,
            stepFeelsLike = pills.map { it.local to feelsLikeC(it.temperatureC, it.humidityPct) },
            days = days,
            rainTomorrowMm = byDate[ctx.today.plusDays(1)]?.sumOf { it.rainMm },
            source = DataSource.BMKG,
        )
    }

    private fun bmkgDay(date: LocalDate, steps: List<BmkgStep>, ctx: Context): DayForecast {
        val daytime = steps.filter { it.local.toLocalTime() in DAYTIME }.ifEmpty { steps }
        return DayForecast(
            date = date,
            lowC = steps.minOf { it.temperatureC },
            highC = steps.maxOf { it.temperatureC },
            condition = daytime.map { bmkgToCondition(it.weatherCode) }.maxBy { SEVERITY.indexOf(it) },
            rainChancePct = ctx.omDays[date]?.rainChancePct,
            source = DataSource.BMKG,
        )
    }

    private fun fromOpenMeteo(ctx: Context): Core {
        val hours = ctx.om.hours.sortedBy { it.time }
        require(hours.isNotEmpty()) { "No BMKG steps and no Open-Meteo hours to merge" }

        val current = hours.lastOrNull { it.time <= ctx.now } ?: hours.first()
        val nowValue = Now(
            temperatureC = current.temperatureC,
            feelsLikeC = current.apparentC,
            humidityPct = current.humidityPct,
            condition = current.condition(),
            windKmh = current.windKmh,
            windFromDeg = current.windFromDeg,
            isDay = ctx.isDay(ctx.now),
        )

        // Every third hour from the current one, so the row looks like BMKG's.
        val windowEnd = current.time.plusHours(STEPS_SPAN_HOURS)
        val pills = hours.filter {
            it.time >= current.time && it.time < windowEnd &&
                Duration.between(current.time, it.time).toHours() % STEP_HOURS == 0L
        }
        val forecastSteps = pills.map {
            ForecastStep(
                time = it.time,
                temperatureC = it.temperatureC,
                condition = it.condition(),
                isDay = ctx.isDay(it.time),
                rainChancePct = it.rainChancePct,
            )
        }

        val todayHours = hours.filter { it.time.toLocalDate() == ctx.today }
        val omToday = ctx.omDays[ctx.today]
        val tomorrow = ctx.today.plusDays(1)

        return Core(
            now = nowValue,
            todayHighC = omToday?.highC ?: todayHours.maxOfOrNull { it.temperatureC } ?: current.temperatureC,
            todayLowC = omToday?.lowC ?: todayHours.minOfOrNull { it.temperatureC } ?: current.temperatureC,
            steps = forecastSteps,
            stepFeelsLike = pills.map { it.time to it.apparentC },
            days = (0 until FORECAST_DAYS).mapNotNull { ctx.omDayForecast(ctx.today.plusDays(it)) },
            rainTomorrowMm = hours.filter { it.time.toLocalDate() == tomorrow }
                .takeIf { it.isNotEmpty() }
                ?.sumOf { it.precipitationMm },
            source = DataSource.OPEN_METEO,
        )
    }

    private fun details(ctx: Context, rainTomorrowMm: Double?): Details {
        val now = ctx.now
        val hours = ctx.om.hours
        val current = ctx.omHours[now.truncatedTo(ChronoUnit.HOURS)]
            ?: hours.minByOrNull { abs(Duration.between(it.time, now).toMinutes()) }

        // Open-Meteo precipitation is the total for the hour ending at `time`.
        val last24h = hours.filter { it.time > now.minusHours(24) && it.time <= now }
        val sunDay = ctx.omDays[ctx.today] ?: ctx.om.days.firstOrNull()

        return Details(
            uvIndex = current?.uvIndex,
            dewPointC = current?.dewPointC,
            gustKmh = current?.gustKmh,
            rainLast24hMm = last24h.takeIf { it.isNotEmpty() }?.sumOf { it.precipitationMm },
            rainTomorrowMm = rainTomorrowMm,
            sunrise = sunDay?.sunrise ?: LocalTime.of(6, 0),
            sunset = sunDay?.sunset ?: LocalTime.of(18, 0),
        )
    }

    private fun lerp(a: Double, b: Double?, fraction: Double): Double =
        if (b == null) a else a + (b - a) * fraction

    private fun OmHour.condition(): Condition =
        wmoToCondition(weatherCode, visibilityM, raining = precipitationMm > 0.0)
}

/** BMKG weather code to condition; unknown codes are Cloudy. Duplicates Task 2's table until integration. */
internal fun bmkgToCondition(code: Int): Condition = when (code) {
    0 -> Condition.CLEAR
    1, 2 -> Condition.PARTLY_CLOUDY
    3, 4 -> Condition.CLOUDY
    5, 10, 45 -> Condition.HAZE
    60, 61, 63, 80 -> Condition.RAIN
    95, 97 -> Condition.THUNDERSTORM
    else -> Condition.CLOUDY
}

/**
 * WMO weather code to condition (Open-Meteo fallback). No rain with visibility under 5 km is Haze;
 * other unknown codes are Cloudy. Duplicates Task 2's table until integration.
 */
internal fun wmoToCondition(code: Int, visibilityM: Double?, raining: Boolean): Condition = when (code) {
    in 95..99 -> Condition.THUNDERSTORM
    in 51..67, in 80..82 -> Condition.RAIN
    45, 48 -> Condition.HAZE
    else -> when {
        !raining && visibilityM != null && visibilityM < 5_000.0 -> Condition.HAZE
        code == 0 -> Condition.CLEAR
        code == 1 || code == 2 -> Condition.PARTLY_CLOUDY
        else -> Condition.CLOUDY
    }
}
