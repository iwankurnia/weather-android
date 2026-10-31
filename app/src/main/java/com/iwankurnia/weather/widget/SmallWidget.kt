package com.iwankurnia.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.iwankurnia.weather.ui.icons.conditionLabel

/** The 2×2 widget: area, temperature, "feels like" pill and condition. */
class SmallWidget : GlanceAppWidget() {
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

@Composable
private fun SmallWidgetContent(state: WidgetState, style: WidgetTextStyle) {
    val colours = widgetColours(style)
    val snapshot = state.snapshot ?: return SetUpPrompt(colours)
    val now = snapshot.now
    Column(modifier = GlanceModifier.widgetCard(colours, 14.dp)) {
        Text(
            text = state.areaLine(),
            style = textStyle(colours.subtle, 11.sp, FontWeight.Medium),
            maxLines = 2,
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(formatDegrees(now.temperatureC), style = textStyle(colours.text, 40.sp), maxLines = 1)
        Spacer(GlanceModifier.height(4.dp))
        FeelsPill(snapshot, colours)
        Spacer(GlanceModifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(snapshot, colours, 16.dp)
            Spacer(GlanceModifier.width(4.dp))
            Text(conditionLabel(now.condition, now.isDay), style = textStyle(colours.text, 11.sp), maxLines = 1)
        }
    }
}
