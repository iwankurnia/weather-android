package com.iwankurnia.weather.ui.background

import androidx.compose.ui.graphics.Color

/** Card and accent colours that suit one weather background. Produced by `cardTint` (plan Task 4). */
data class CardColours(
    val surface: Color,
    val onSurface: Color,
    val subtle: Color,
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
    /** Text drawn straight on the sky (hero temperature, headings). */
    val onSky: Color,
)
