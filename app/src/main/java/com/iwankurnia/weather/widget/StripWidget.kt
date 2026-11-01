package com.iwankurnia.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import com.iwankurnia.weather.model.WeatherSnapshot

/**
 * The 4×1 "today" strip: icon, temperature and "feels like", then the forecast summary split
 * into a headline ("Rain likely from around 16:00") and a detail ("Very hot until then").
 * Narrower than about three cells it keeps just the weather. System colours, like the others.
 */
class StripWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialState = loadWidgetState(context)
        val initialOpacity = WidgetPrefs.opacity(context, appWidgetId)
        provideContent {
            val (state, opacity) = rememberWidgetInputs(context, appWidgetId, initialState, initialOpacity)
            GlanceTheme { StripContent(state, widgetTextStyle(opacity)) }
        }
    }
}

/** Below this width (dp) the summary no longer fits beside the weather. */
private const val STRIP_SUMMARY_MIN_DP = 240f

internal fun stripShowsSummary(widthDp: Float): Boolean = widthDp >= STRIP_SUMMARY_MIN_DP

/** "Rain likely from around 16:00. Very hot until then." → headline and detail, without full stops. */
internal fun stripLines(summary: String): Pair<String, String?> {
    val sentences = summary.split(". ").map { it.trim().removeSuffix(".") }.filter { it.isNotEmpty() }
    return (sentences.firstOrNull() ?: "") to sentences.drop(1).joinToString(". ").ifEmpty { null }
}

@Composable
private fun StripContent(state: WidgetState, style: WidgetTextStyle) {
    val colours = widgetColours(style)
    val snapshot = state.snapshot ?: return Placeholder(state.placeholderText(), colours)
    Row(modifier = GlanceModifier.widgetCard(colours, 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(GlanceModifier.width(2.dp))
        WeatherIcon(snapshot, colours, 30.dp)
        Spacer(GlanceModifier.width(6.dp))
        Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(colours.text, 24.sp), maxLines = 1)
        Spacer(GlanceModifier.width(8.dp))
        FeelsPill(snapshot, colours)
        if (stripShowsSummary(LocalSize.current.width.value)) Summary(snapshot, colours)
    }
}

@Composable
private fun RowScope.Summary(snapshot: WeatherSnapshot, colours: WidgetColours) {
    val (headline, detail) = stripLines(snapshot.summary)
    Spacer(GlanceModifier.width(10.dp))
    Box(modifier = GlanceModifier.width(1.dp).height(26.dp).background(colours.subtle)) {}
    Spacer(GlanceModifier.width(10.dp))
    Column(modifier = GlanceModifier.defaultWeight()) {
        Text(headline, style = textStyle(colours.text, 12.sp, FontWeight.Bold), maxLines = 1)
        if (detail != null) {
            Text(detail, style = textStyle(colours.subtle, 11.sp), maxLines = 1)
        }
    }
}
