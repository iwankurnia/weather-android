package com.iwankurnia.weather.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
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
 * and "feels like" pill on the right.
 */
class LargeWidget : GlanceAppWidget() {
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

@Composable
private fun LargeWidgetContent(state: WidgetState, style: WidgetTextStyle) {
    val colours = widgetColours(style)
    val snapshot = state.snapshot ?: return Placeholder(state.placeholderText(), colours)
    Column(modifier = GlanceModifier.widgetCard(colours, 16.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = state.areaLine(),
                    style = textStyle(colours.subtle, 11.sp, FontWeight.Medium),
                    maxLines = 1,
                )
                state.staleHint()?.let { hint ->
                    Spacer(GlanceModifier.height(2.dp))
                    StaleHint(hint, colours)
                }
            }
            Text(
                text = todayLine(snapshot),
                style = textStyle(colours.text, 11.sp, FontWeight.Medium, TextAlign.End),
                maxLines = 1,
            )
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AndroidRemoteViews(
                remoteViews = clockViews(LocalContext.current, style, colours),
                modifier = GlanceModifier.defaultWeight(),
            )
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherIcon(snapshot, colours, 30.dp)
                    Spacer(GlanceModifier.width(4.dp))
                    Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(colours.text, 34.sp), maxLines = 1)
                }
                Spacer(GlanceModifier.height(4.dp))
                FeelsPill(snapshot, colours)
            }
        }
    }
}

/** "Partly cloudy · H34° L25°". */
private fun todayLine(snapshot: WeatherSnapshot): String {
    val now = snapshot.now
    return "${conditionLabel(now.condition, now.isDay)} · " +
        "H${formatDegrees(snapshot.todayHighC)} L${formatDegrees(snapshot.todayLowC)}"
}

/** The TextClock pair, shadowed white at low opacity, otherwise in the system text colour. */
private fun clockViews(context: Context, style: WidgetTextStyle, colours: WidgetColours): RemoteViews =
    RemoteViews(context.packageName, R.layout.widget_clock).apply {
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
    }
