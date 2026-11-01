package com.iwankurnia.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.icons.conditionLabel

/**
 * The 4×2 widget on the app's own weather background instead of system colours: a still frame of
 * the sky for the current weather and time of day, redrawn with each refresh (so the rain and the
 * bolt land somewhere new). Same content and resizing as the large widget; the opacity setting
 * fades the scene, leaving white text on the wallpaper at 0%.
 */
class SceneWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val initialState = loadWidgetState(context)
        val initialOpacity = WidgetPrefs.opacity(context, appWidgetId)
        provideContent {
            val (state, opacity) = rememberWidgetInputs(context, appWidgetId, initialState, initialOpacity)
            GlanceTheme { SceneContent(state, opacity) }
        }
    }
}

/** White text, with translucent white pills, over the scene. */
private val SceneColours = WidgetColours(
    background = ColorProvider(Color.Transparent),
    text = ColorProvider(Color.White),
    subtle = ColorProvider(Color(0xE6FFFFFF)),
    pill = ColorProvider(Color(0x38FFFFFF)),
    onPill = ColorProvider(Color.White),
    iconBackdrop = null,
    textDayNight = 0xFFFFFFFF.toInt() to 0xFFFFFFFF.toInt(),
)

/** The scene's clock is the shadowed white TextClock, whatever the opacity. */
private val SceneClockStyle = widgetTextStyle(0)

@Composable
private fun SceneContent(state: WidgetState, opacityPct: Int) {
    val snapshot = state.snapshot
        ?: return Placeholder(state.placeholderText(), widgetColours(widgetTextStyle(100)))
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val now = snapshot.now
    val bitmap = remember(snapshot.fetchedAt, now.condition, now.isDay, size, opacityPct) {
        sceneBitmapSizeOrNull(size.width.value, size.height.value, density)?.let { (w, h) ->
            renderScene(now.condition, now.isDay, w, h, seed = snapshot.fetchedAt.epochSecond.toInt(), alpha = opacityPct / 100f)
        }
    }
    Box(modifier = GlanceModifier.fillMaxSize().systemWidgetCorners().opensApp()) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
        when (largeLayoutFor(size.width.value, size.height.value)) {
            LargeLayout.ROW -> SceneRow(snapshot)
            LargeLayout.ROOMY -> SceneCard(state, snapshot, ROOMY_SCENE)
            else -> SceneCard(state, snapshot, STANDARD_SCENE)
        }
    }
}

private class SceneScale(val clockSp: Float, val dateSp: Float, val temperature: TextUnit, val label: TextUnit, val padding: Dp, val showSummary: Boolean)

private val STANDARD_SCENE = SceneScale(38f, 12f, 34.sp, 11.sp, 16.dp, showSummary = false)
private val ROOMY_SCENE = SceneScale(52f, 14f, 46.sp, 13.sp, 20.dp, showSummary = true)

/** One cell tall: time on the left; temperature and "feels like" on the right. */
@Composable
private fun SceneRow(snapshot: WeatherSnapshot) {
    Row(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        AndroidRemoteViews(
            remoteViews = clockViews(LocalContext.current, SceneClockStyle, SceneColours, timeSp = 30f, dateSp = 0f),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(formatDegrees(snapshot.now.temperatureC), style = textStyle(SceneColours.text, 30.sp), maxLines = 1)
        Spacer(GlanceModifier.width(8.dp))
        FeelsPill(snapshot, SceneColours)
    }
}

@Composable
private fun SceneCard(state: WidgetState, snapshot: WeatherSnapshot, scale: SceneScale) {
    val now = snapshot.now
    Column(modifier = GlanceModifier.fillMaxSize().padding(scale.padding)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(state.areaLine(), style = textStyle(SceneColours.text, scale.label, FontWeight.Medium), maxLines = 1)
                state.staleHint()?.let { hint ->
                    Spacer(GlanceModifier.height(2.dp))
                    Text(hint, style = textStyle(SceneColours.subtle, 10.sp, FontWeight.Medium), maxLines = 1)
                }
            }
            Text(
                "${conditionLabel(now.condition, now.isDay)} · H${formatDegrees(snapshot.todayHighC)} L${formatDegrees(snapshot.todayLowC)}",
                style = textStyle(SceneColours.text, scale.label, FontWeight.Medium, TextAlign.End),
                maxLines = 1,
            )
        }
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            AndroidRemoteViews(
                remoteViews = clockViews(LocalContext.current, SceneClockStyle, SceneColours, scale.clockSp, scale.dateSp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDegrees(now.temperatureC), style = textStyle(SceneColours.text, scale.temperature), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                FeelsPill(snapshot, SceneColours)
            }
        }
        if (scale.showSummary) {
            Text(snapshot.summary, style = textStyle(SceneColours.subtle, scale.label, FontWeight.Medium), maxLines = 2)
        }
    }
}
