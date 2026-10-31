package com.iwankurnia.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.icons.conditionLabel

/**
 * The 2×2 widget: area, temperature, "feels like" pill and condition. When the location may be
 * out of date, a smaller hint replaces the condition text. Resizing switches layout; see
 * [SmallLayout].
 */
class SmallWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialState = loadWidgetState(context)
        val initialOpacity = WidgetPrefs.opacity(context, appWidgetId)
        provideContent {
            val (state, opacity) = rememberWidgetInputs(context, appWidgetId, initialState, initialOpacity)
            GlanceTheme { SmallWidgetContent(state, widgetTextStyle(opacity)) }
        }
    }
}

/** Type and spacing for the card layouts. */
private class SmallScale(val label: TextUnit, val temperature: TextUnit, val icon: Dp, val padding: Dp, val showHighLow: Boolean)

private val STANDARD = SmallScale(11.sp, 40.sp, 16.dp, 14.dp, showHighLow = false)
private val ROOMY = SmallScale(13.sp, 56.sp, 20.dp, 18.dp, showHighLow = true)

@Composable
private fun SmallWidgetContent(state: WidgetState, style: WidgetTextStyle) {
    val colours = widgetColours(style)
    val snapshot = state.snapshot ?: return Placeholder(state.placeholderText(), colours)
    val size = LocalSize.current
    when (smallLayoutFor(size.width.value, size.height.value)) {
        SmallLayout.MINI -> SmallMini(snapshot, colours, showFeels = size.height.value >= 110f)
        SmallLayout.ROW -> SmallRow(state, snapshot, colours)
        SmallLayout.STANDARD -> SmallCard(state, snapshot, colours, STANDARD)
        SmallLayout.ROOMY -> SmallCard(state, snapshot, colours, ROOMY)
    }
}

/** One cell wide: icon over temperature, plus "feels like" when there is height for it. */
@Composable
private fun SmallMini(snapshot: WeatherSnapshot, colours: WidgetColours, showFeels: Boolean) {
    Column(
        modifier = GlanceModifier.widgetCard(colours, 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WeatherIcon(snapshot, colours, 22.dp)
        Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(colours.text, 24.sp), maxLines = 1)
        if (showFeels) {
            Text(
                "Feels ${formatDegrees(snapshot.now.feelsLikeC)}",
                style = textStyle(colours.subtle, 10.sp, FontWeight.Medium),
                maxLines = 1,
            )
        }
    }
}

/** One cell tall: icon and temperature, then "feels like" over the area (or the stale hint). */
@Composable
private fun SmallRow(state: WidgetState, snapshot: WeatherSnapshot, colours: WidgetColours) {
    Row(modifier = GlanceModifier.widgetCard(colours, 12.dp), verticalAlignment = Alignment.CenterVertically) {
        WeatherIcon(snapshot, colours, 28.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(colours.text, 30.sp), maxLines = 1)
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            FeelsPill(snapshot, colours)
            Spacer(GlanceModifier.height(3.dp))
            val hint = state.staleHint()
            if (hint != null) {
                StaleHint(hint, colours)
            } else {
                Text(state.areaLine(), style = textStyle(colours.subtle, 10.sp, FontWeight.Medium), maxLines = 1)
            }
        }
    }
}

@Composable
private fun SmallCard(state: WidgetState, snapshot: WeatherSnapshot, colours: WidgetColours, scale: SmallScale) {
    val now = snapshot.now
    Column(modifier = GlanceModifier.widgetCard(colours, scale.padding)) {
        Text(
            text = state.areaLine(),
            style = textStyle(colours.subtle, scale.label, FontWeight.Medium),
            maxLines = 2,
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(formatDegrees(now.temperatureC), style = textStyle(colours.text, scale.temperature), maxLines = 1)
        Spacer(GlanceModifier.height(4.dp))
        FeelsPill(snapshot, colours)
        Spacer(GlanceModifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(snapshot, colours, scale.icon)
            Spacer(GlanceModifier.width(4.dp))
            // No room for both: the hint takes the condition's place, which the icon still shows.
            val hint = state.staleHint()
            if (hint != null) {
                StaleHint(hint, colours, maxLines = 2)
            } else {
                Text(conditionLabel(now.condition, now.isDay), style = textStyle(colours.text, scale.label), maxLines = 1)
            }
        }
        if (scale.showHighLow) {
            Spacer(GlanceModifier.height(3.dp))
            Text(
                "H${formatDegrees(snapshot.todayHighC)} L${formatDegrees(snapshot.todayLowC)}",
                style = textStyle(colours.subtle, scale.label, FontWeight.Medium),
                maxLines = 1,
            )
        }
    }
}
