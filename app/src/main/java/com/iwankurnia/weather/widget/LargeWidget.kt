package com.iwankurnia.weather.widget

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.iwankurnia.weather.R
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.icons.conditionLabel

/**
 * The 4×2 widget: area (with a small hint under it when the location may be out of date) and
 * today's conditions on top; the ticking time and date on the left, and the temperature, icon
 * and "feels like" pill on the right. Resizing switches layout; see [LargeLayout].
 */
class LargeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialState = loadWidgetState(context)
        val initialOpacity = WidgetPrefs.opacity(context, appWidgetId)
        provideContent {
            val (state, opacity) = rememberWidgetInputs(context, appWidgetId, initialState, initialOpacity)
            GlanceTheme { LargeWidgetContent(state, widgetTextStyle(opacity)) }
        }
    }
}

/** Type and spacing for one card layout. */
private class LargeScale(
    val clockSp: Float,
    val dateSp: Float,
    val temperature: TextUnit,
    val icon: Dp,
    val label: TextUnit,
    val padding: Dp,
    val showToday: Boolean,
    val showSummary: Boolean,
)

private val NARROW = LargeScale(30f, 11f, 28.sp, 24.dp, 11.sp, 14.dp, showToday = false, showSummary = false)
private val STANDARD = LargeScale(38f, 12f, 34.sp, 30.dp, 11.sp, 16.dp, showToday = true, showSummary = false)
private val ROOMY = LargeScale(52f, 14f, 46.sp, 40.dp, 13.sp, 20.dp, showToday = true, showSummary = true)

@Composable
private fun LargeWidgetContent(state: WidgetState, style: WidgetTextStyle) {
    val colours = widgetColours(style)
    val snapshot = state.snapshot ?: return Placeholder(state.placeholderText(), colours)
    val size = LocalSize.current
    when (largeLayoutFor(size.width.value, size.height.value)) {
        LargeLayout.ROW -> LargeRow(snapshot, style, colours)
        LargeLayout.NARROW -> LargeCard(state, snapshot, style, colours, NARROW)
        LargeLayout.STANDARD -> LargeCard(state, snapshot, style, colours, STANDARD)
        LargeLayout.ROOMY -> LargeCard(state, snapshot, style, colours, ROOMY)
    }
}

/** 4×1: time on the left; icon, temperature and "feels like" on the right. */
@Composable
private fun LargeRow(snapshot: WeatherSnapshot, style: WidgetTextStyle, colours: WidgetColours) {
    Row(modifier = GlanceModifier.widgetCard(colours, 14.dp), verticalAlignment = Alignment.CenterVertically) {
        AndroidRemoteViews(
            remoteViews = clockViews(LocalContext.current, style, colours, timeSp = 30f, dateSp = 0f),
            modifier = GlanceModifier.defaultWeight(),
        )
        WeatherIcon(snapshot, colours, 26.dp)
        Spacer(GlanceModifier.width(4.dp))
        Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(colours.text, 30.sp), maxLines = 1)
        Spacer(GlanceModifier.width(8.dp))
        FeelsPill(snapshot, colours)
    }
}

@Composable
private fun LargeCard(
    state: WidgetState,
    snapshot: WeatherSnapshot,
    style: WidgetTextStyle,
    colours: WidgetColours,
    scale: LargeScale,
) {
    Column(modifier = GlanceModifier.widgetCard(colours, scale.padding)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = state.areaLine(),
                    style = textStyle(colours.subtle, scale.label, FontWeight.Medium),
                    maxLines = 1,
                )
                state.staleHint()?.let { hint ->
                    Spacer(GlanceModifier.height(2.dp))
                    StaleHint(hint, colours)
                }
            }
            if (scale.showToday) {
                Text(
                    text = todayLine(snapshot),
                    style = textStyle(colours.text, scale.label, FontWeight.Medium, TextAlign.End),
                    maxLines = 1,
                )
            }
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AndroidRemoteViews(
                remoteViews = clockViews(LocalContext.current, style, colours, scale.clockSp, scale.dateSp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(snapshot, colours, scale.icon)
                    Spacer(GlanceModifier.width(4.dp))
                    Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(colours.text, scale.temperature), maxLines = 1)
                }
                Spacer(GlanceModifier.height(4.dp))
                FeelsPill(snapshot, colours)
            }
        }
        if (scale.showSummary) {
            Text(snapshot.summary, style = textStyle(colours.subtle, scale.label, FontWeight.Medium), maxLines = 2)
        }
    }
}

/** "Partly cloudy · H34° L25°". */
private fun todayLine(snapshot: WeatherSnapshot): String {
    val now = snapshot.now
    return "${conditionLabel(now.condition, now.isDay)} · " +
        "H${formatDegrees(snapshot.todayHighC)} L${formatDegrees(snapshot.todayLowC)}"
}

/**
 * The TextClock pair, shadowed white at low opacity, otherwise in the system text colour.
 * [dateSp] of 0 hides the date (one-row layout).
 */
private fun clockViews(
    context: Context,
    style: WidgetTextStyle,
    colours: WidgetColours,
    timeSp: Float,
    dateSp: Float,
): RemoteViews = RemoteViews(context.packageName, R.layout.widget_clock).apply {
    if (style.whiteWithShadow) {
        setViewVisibility(R.id.clock_plain, View.GONE)
        setViewVisibility(R.id.clock_shadow, View.VISIBLE)
    } else {
        setViewVisibility(R.id.clock_plain, View.VISIBLE)
        setViewVisibility(R.id.clock_shadow, View.GONE)
        val (day, night) = colours.textDayNight
        setColorInt(R.id.clock_time, "setTextColor", day, night)
        setColorInt(R.id.clock_date, "setTextColor", day, night)
    }
    for (id in intArrayOf(R.id.clock_time, R.id.clock_time_shadow)) {
        setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, timeSp)
    }
    for (id in intArrayOf(R.id.clock_date, R.id.clock_date_shadow)) {
        if (dateSp > 0f) {
            setViewVisibility(id, View.VISIBLE)
            setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, dateSp)
        } else {
            setViewVisibility(id, View.GONE)
        }
    }
}
