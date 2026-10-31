package com.iwankurnia.weather.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.iwankurnia.weather.R

/*
 * Roboto Flex (OFL, bundled in res/font, licence in assets/licences) is a variable font.
 * Each family below pins the width axis (and optical size for the hero) and offers the
 * weights the screen uses, each with a matching `wght` setting.
 */

private val flexWeights = listOf(300, 350, 400, 500, 600, 700)

@OptIn(ExperimentalTextApi::class)
private fun robotoFlex(width: Float, opticalSize: Float? = null): FontFamily = FontFamily(
    flexWeights.map { weight ->
        val settings = listOfNotNull(
            FontVariation.weight(weight),
            FontVariation.width(width),
            opticalSize?.let { FontVariation.Setting("opsz", it) },
        )
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(*settings.toTypedArray()),
        )
    },
)

/** Normal width, for body text. */
val RobotoFlex: FontFamily = robotoFlex(width = 100f)
private val Flex105 = robotoFlex(width = 105f)
private val Flex110 = robotoFlex(width = 110f)
private val Flex115 = robotoFlex(width = 115f)
private val Flex120 = robotoFlex(width = 120f)

/** Wide display cut (wdth 125, opsz 144) for the big temperature. */
private val FlexHero = robotoFlex(width = 125f, opticalSize = 144f)

/** Material type scale with Roboto Flex everywhere. */
val WeatherTypography: Typography = Typography().let { base ->
    fun TextStyle.flex() = copy(fontFamily = RobotoFlex)
    Typography(
        displayLarge = base.displayLarge.flex(),
        displayMedium = base.displayMedium.flex(),
        displaySmall = base.displaySmall.flex(),
        headlineLarge = base.headlineLarge.flex(),
        headlineMedium = base.headlineMedium.flex(),
        headlineSmall = base.headlineSmall.flex(),
        titleLarge = base.titleLarge.flex(),
        titleMedium = base.titleMedium.flex(),
        titleSmall = base.titleSmall.flex(),
        bodyLarge = base.bodyLarge.flex(),
        bodyMedium = base.bodyMedium.flex(),
        bodySmall = base.bodySmall.flex(),
        labelLarge = base.labelLarge.flex(),
        labelMedium = base.labelMedium.flex(),
        labelSmall = base.labelSmall.flex(),
    )
}

/** Text styles taken from the mockups (`mockups/app-screen.html`, `all-screens.html`). */
object WeatherText {
    val heroTemperature = TextStyle(
        fontFamily = FlexHero,
        fontWeight = FontWeight(350),
        fontSize = 108.sp,
        lineHeight = 92.sp,
        letterSpacing = (-0.037).em,
    )
    val condition = TextStyle(fontFamily = Flex115, fontWeight = FontWeight.SemiBold, fontSize = 21.sp)
    val highLow = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp)
    val feelsLabel = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    val feelsValue = TextStyle(
        fontFamily = Flex120,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 32.sp,
    )
    val chip = TextStyle(fontFamily = Flex110, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val summary = TextStyle(fontFamily = Flex105, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val pillLabel = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp)
    val pillTemperature = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val smallStrong = TextStyle(
        fontFamily = RobotoFlex,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
    val sectionHeader = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    val row = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp)
    val rowDay = row.copy(fontWeight = FontWeight.SemiBold)
    val tileLabel = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    val tileBig = TextStyle(
        fontFamily = Flex115,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 32.sp,
    )
    val tileMedium = tileBig.copy(fontSize = 24.sp, lineHeight = 26.sp)
    val tileSmall = TextStyle(fontFamily = RobotoFlex, fontSize = 12.sp, lineHeight = 16.sp)
    val badge = TextStyle(fontFamily = Flex115, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val credits = TextStyle(fontFamily = RobotoFlex, fontSize = 11.sp, lineHeight = 15.sp)
    val stateTitle = TextStyle(
        fontFamily = Flex110,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
    )
    val stateBody = TextStyle(fontFamily = RobotoFlex, fontSize = 14.sp, lineHeight = 20.sp)
    val button = TextStyle(fontFamily = RobotoFlex, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val banner = TextStyle(fontFamily = RobotoFlex, fontSize = 13.sp, lineHeight = 17.sp)
}
