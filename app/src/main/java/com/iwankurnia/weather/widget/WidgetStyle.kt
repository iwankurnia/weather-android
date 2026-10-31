package com.iwankurnia.weather.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.iwankurnia.weather.MainActivity
import com.iwankurnia.weather.data.weatherRepositoryProvider
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.icons.conditionLabel
import com.iwankurnia.weather.ui.icons.weatherIconRes
import kotlin.math.roundToInt
import androidx.glance.color.ColorProvider as dayNightColorProvider

/** Opacity at or below which widget text turns white over the wallpaper. */
const val WHITE_TEXT_MAX_OPACITY = 40

/**
 * How a widget is drawn for its background opacity.
 *
 * At 40% or lower ([whiteWithShadow]) the mockup turns the text white with a soft shadow.
 * Glance has no text shadow, so the widgets approximate it: white text, plus a subtle dark
 * translucent pill behind the weather icon and the "feels like" pill. The clock in the large
 * widget is a real `TextClock`, so it gets a true shadow from `widget_clock.xml`.
 */
data class WidgetTextStyle(
    /** Background alpha, 0 to 1. */
    val backgroundAlpha: Float,
    val whiteWithShadow: Boolean,
)

fun widgetTextStyle(opacityPct: Int): WidgetTextStyle {
    val pct = opacityPct.coerceIn(0, 100)
    return WidgetTextStyle(backgroundAlpha = pct / 100f, whiteWithShadow = pct <= WHITE_TEXT_MAX_OPACITY)
}

/** Whole degrees, e.g. 33.4 → "33°", never "-0°". */
fun formatDegrees(celsius: Double): String = "${celsius.roundToInt()}°"

/** What a widget needs from outside its own settings. */
internal data class WidgetState(
    /** Null when the app hasn't been set up or has nothing cached yet. */
    val snapshot: WeatherSnapshot?,
    /** True without "Allow all the time" location: the area may be where the phone last was. */
    val locationMayBeStale: Boolean,
)

internal suspend fun loadWidgetState(context: Context): WidgetState {
    val snapshot = runCatching { context.weatherRepositoryProvider()?.snapshotCache?.snapshot() }.getOrNull()
    val background = context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return WidgetState(snapshot, locationMayBeStale = !background)
}

/** Text shown in place of the area name when background location is refused. */
internal const val LOCATION_STALE_TEXT = "Location may be out of date"

internal const val SET_UP_TEXT = "Open Weather to set up"

internal fun WidgetState.areaLine(): String =
    if (locationMayBeStale) LOCATION_STALE_TEXT else snapshot?.place?.areaName.orEmpty()

/**
 * Collects the widget's state and opacity so a running Glance session redraws when the cache
 * is refreshed ([WidgetUpdater.refreshes]) or the opacity changes.
 */
@Composable
internal fun rememberWidgetInputs(
    context: Context,
    appWidgetId: Int,
    initialState: WidgetState,
    initialOpacity: Int,
): Pair<WidgetState, Int> {
    val refreshes by WidgetUpdater.refreshes.collectAsState()
    val state by produceState(initialState, refreshes) { value = loadWidgetState(context) }
    val opacityFlow = remember(appWidgetId) { WidgetPrefs.opacityFlow(context, appWidgetId) }
    val opacity by opacityFlow.collectAsState(initialOpacity)
    return state to opacity
}

/** The widget's colours: the system (wallpaper) scheme, or white over the wallpaper at low opacity. */
internal class WidgetColours(
    val background: ColorProvider,
    val text: ColorProvider,
    val subtle: ColorProvider,
    val pill: ColorProvider,
    val onPill: ColorProvider,
    /** Dark translucent backdrop behind the icon, standing in for a shadow; null when not needed. */
    val iconBackdrop: ColorProvider?,
    /** Text colour as day and night ARGB ints, for the RemoteViews clock. */
    val textDayNight: Pair<Int, Int>,
)

@Composable
internal fun widgetColours(style: WidgetTextStyle): WidgetColours {
    val context = LocalContext.current
    val theme = GlanceTheme.colors
    // Surface, as in the mockup; kept as the live dynamic colour when fully opaque.
    val background = if (style.backgroundAlpha >= 1f) {
        theme.surface
    } else {
        theme.surface.withAlpha(context, style.backgroundAlpha)
    }
    return if (style.whiteWithShadow) {
        val white = ColorProvider(Color.White)
        WidgetColours(
            background = background,
            text = white,
            subtle = white,
            pill = ColorProvider(SHADOW_PILL),
            onPill = white,
            iconBackdrop = ColorProvider(SHADOW_BACKDROP),
            textDayNight = Color.White.toArgb() to Color.White.toArgb(),
        )
    } else {
        WidgetColours(
            background = background,
            text = theme.onSurface,
            subtle = theme.onSurfaceVariant,
            pill = theme.primaryContainer,
            onPill = theme.onPrimaryContainer,
            iconBackdrop = null,
            textDayNight = theme.onSurface.dayNightArgb(context),
        )
    }
}

private val SHADOW_PILL = Color(0x40000000)
private val SHADOW_BACKDROP = Color(0x2E000000)

/** Resolves a (possibly dynamic, day/night) colour and reapplies it with [alpha] for both modes. */
private fun ColorProvider.withAlpha(context: Context, alpha: Float): ColorProvider {
    val day = getColor(context.withNightMode(false)).copy(alpha = alpha)
    val night = getColor(context.withNightMode(true)).copy(alpha = alpha)
    return dayNightColorProvider(day = day, night = night)
}

private fun ColorProvider.dayNightArgb(context: Context): Pair<Int, Int> =
    getColor(context.withNightMode(false)).toArgb() to getColor(context.withNightMode(true)).toArgb()

private fun Context.withNightMode(night: Boolean): Context {
    val config = Configuration(resources.configuration)
    val mode = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
    return createConfigurationContext(config)
}

internal fun textStyle(
    colour: ColorProvider,
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
) = TextStyle(color = colour, fontSize = size, fontWeight = weight, textAlign = align)

/** Opens the app; every part of both widgets uses it. */
internal fun GlanceModifier.opensApp(): GlanceModifier = clickable(actionStartActivity<MainActivity>())

/** The rounded card behind each widget, with the opacity applied to its colour. */
internal fun GlanceModifier.widgetCard(colours: WidgetColours, padding: Dp): GlanceModifier =
    fillMaxSize().cornerRadius(26.dp).background(colours.background).padding(padding).opensApp()

@Composable
internal fun WeatherIcon(snapshot: WeatherSnapshot, colours: WidgetColours, size: Dp) {
    val now = snapshot.now
    val image = @Composable {
        Image(
            provider = ImageProvider(weatherIconRes(now.condition, now.isDay)),
            contentDescription = conditionLabel(now.condition, now.isDay),
            modifier = GlanceModifier.size(size),
        )
    }
    val backdrop = colours.iconBackdrop
    if (backdrop == null) {
        image()
    } else {
        val pad = 3.dp
        Box(
            modifier = GlanceModifier.background(backdrop).cornerRadius(size / 2 + pad).padding(pad),
            contentAlignment = Alignment.Center,
        ) { image() }
    }
}

/** "Feels 39°" in a pill. */
@Composable
internal fun FeelsPill(snapshot: WeatherSnapshot, colours: WidgetColours) {
    Box(
        modifier = GlanceModifier.background(colours.pill).cornerRadius(999.dp).padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text = "Feels ${formatDegrees(snapshot.now.feelsLikeC)}",
            style = textStyle(colours.onPill, 11.sp, FontWeight.Medium),
            maxLines = 1,
        )
    }
}

/** Shown before the app has been opened or wired up. */
@Composable
internal fun SetUpPrompt(colours: WidgetColours) {
    Box(modifier = GlanceModifier.widgetCard(colours, 16.dp), contentAlignment = Alignment.Center) {
        Text(SET_UP_TEXT, style = textStyle(colours.text, 14.sp, FontWeight.Medium, TextAlign.Center))
    }
}
