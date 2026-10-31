package com.iwankurnia.weather.domain

import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.model.ForecastStep
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The one-line forecast summary over the next [SUMMARY_LOOKAHEAD_HOURS] of [steps].
 *
 * [feelsLike] gives "feels like" per step time; a step without one never counts as very hot.
 * When heat ends before the rain starts, only the rain is mentioned.
 */
fun summaryLine(steps: List<ForecastStep>, feelsLike: List<Pair<LocalDateTime, Double>>): String {
    val dry = "Dry and warm for the next few hours."
    val first = steps.firstOrNull() ?: return dry
    val window = steps.filter { it.time < first.time.plusHours(SUMMARY_LOOKAHEAD_HOURS) }

    val feels = feelsLike.toMap()
    fun isVeryHot(step: ForecastStep) = (feels[step.time] ?: Double.NEGATIVE_INFINITY) >= VERY_HOT_FEELS_C

    val rainIndex = window.indexOfFirst(::isRainLikely)
    val coolIndex = window.indexOfFirst { !isVeryHot(it) }
    val hotNow = coolIndex != 0

    return when {
        rainIndex > 0 && hotNow && (coolIndex == -1 || coolIndex >= rainIndex) ->
            "Rain likely from around ${window[rainIndex].time.format(hourMinute)}. Very hot until then."
        rainIndex >= 0 -> "Rain likely from around ${window[rainIndex].time.format(hourMinute)}."
        hotNow && coolIndex == -1 -> "Very hot for the next few hours."
        hotNow -> "Very hot until ${window[coolIndex].time.format(hourMinute)}."
        else -> dry
    }
}

private fun isRainLikely(step: ForecastStep): Boolean =
    step.condition == Condition.RAIN ||
        step.condition == Condition.THUNDERSTORM ||
        (step.rainChancePct ?: 0) >= RAIN_LIKELY_PCT
