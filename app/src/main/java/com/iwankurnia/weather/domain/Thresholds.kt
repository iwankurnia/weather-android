package com.iwankurnia.weather.domain

// The only place these tuning values live, so they are easy to adjust after a week of use.

/** Open-Meteo chance of rain (%) at or above which rain counts as "likely". */
const val RAIN_LIKELY_PCT = 50

/** "Feels like" (°C) at or above which it counts as "very hot". */
const val VERY_HOT_FEELS_C = 38.0

/** How far ahead the forecast summary line looks. */
const val SUMMARY_LOOKAHEAD_HOURS = 12L

/** Furthest the nearest kelurahan may be before BMKG is skipped for Open-Meteo. */
const val BMKG_MAX_KM = 15.0
