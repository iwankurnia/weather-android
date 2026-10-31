package com.iwankurnia.weather.widget

/**
 * Layouts the 4×2 widget switches between as it is resized: one row when short (4×1), a
 * narrower card without the condition text (3×2), the agreed card (4×2), and a roomier card
 * with bigger type and the forecast summary when there is space (4×3 and up).
 */
internal enum class LargeLayout { ROW, NARROW, STANDARD, ROOMY }

/**
 * Layouts the 2×2 widget switches between: icon and temperature only (1×1, or one cell wide),
 * a single row (2×1 and wider), the agreed card (2×2), and bigger type with today's high and
 * low (3×3 and up).
 */
internal enum class SmallLayout { MINI, ROW, STANDARD, ROOMY }

/**
 * Below this size (dp) a widget is one cell tall or wide. The two-row cards need about 120 dp,
 * and one Pixel-launcher cell is ~103 dp, so the cut-off sits just above that.
 */
private const val ONE_ROW_MAX_DP = 120f

internal fun largeLayoutFor(widthDp: Float, heightDp: Float): LargeLayout = when {
    heightDp < ONE_ROW_MAX_DP -> LargeLayout.ROW
    widthDp < 240f -> LargeLayout.NARROW
    widthDp >= 270f && heightDp >= 170f -> LargeLayout.ROOMY
    else -> LargeLayout.STANDARD
}

internal fun smallLayoutFor(widthDp: Float, heightDp: Float): SmallLayout = when {
    widthDp < ONE_ROW_MAX_DP -> SmallLayout.MINI
    heightDp < ONE_ROW_MAX_DP -> SmallLayout.ROW
    widthDp >= 170f && heightDp >= 170f -> SmallLayout.ROOMY
    else -> SmallLayout.STANDARD
}
