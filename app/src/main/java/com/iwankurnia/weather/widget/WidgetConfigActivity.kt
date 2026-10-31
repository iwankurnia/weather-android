package com.iwankurnia.weather.widget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.iwankurnia.weather.data.weatherRepositoryProvider
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.icons.conditionLabel
import com.iwankurnia.weather.ui.icons.weatherIconRes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Opens when a widget is added or reconfigured (long-press, then the pencil).
 *
 * Without "Allow all the time" location it first explains why it is needed; "Continue" asks for
 * it (Android 11+ opens the settings page), "Not now" skips. Then the opacity settings with a
 * live preview; "Done" saves, redraws the widget and confirms the configuration.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out of a first configuration cancels adding the widget.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val small = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
            ?.provider?.className == SmallWidgetReceiver::class.java.name

        enableEdgeToEdge()
        setContent {
            ConfigTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    small = small,
                    needsBackgroundLocation = !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    hasForegroundLocation = { hasForegroundLocation() },
                    onDone = ::finishWithOpacity,
                )
            }
        }
    }

    private fun finishWithOpacity(opacityPct: Int) {
        lifecycleScope.launch {
            val context = applicationContext
            WidgetPrefs.setOpacity(context, appWidgetId, opacityPct)
            val widget: GlanceAppWidget = if (isSmallWidget()) SmallWidget() else LargeWidget()
            runCatching { widget.update(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)) }
            WidgetUpdater.schedule(context)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun isSmallWidget(): Boolean = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        ?.provider?.className == SmallWidgetReceiver::class.java.name

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun hasPermission(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun hasForegroundLocation() =
        hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
}

private enum class ConfigStep { EXPLAIN, SETTINGS }

private val PRESETS = listOf(0, 30, 60, 100)

@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    small: Boolean,
    needsBackgroundLocation: Boolean,
    hasForegroundLocation: () -> Boolean,
    onDone: (Int) -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableStateOf(if (needsBackgroundLocation) ConfigStep.EXPLAIN else ConfigStep.SETTINGS) }
    var opacity by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(appWidgetId) {
        if (opacity == null) opacity = WidgetPrefs.opacity(context, appWidgetId)
    }
    val snapshot by produceState<WeatherSnapshot?>(null) {
        value = runCatching { context.weatherRepositoryProvider()?.snapshotCache?.snapshot() }.getOrNull()
    }

    // Whatever the answer, carry on to the opacity settings.
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        step = ConfigStep.SETTINGS
    }
    // Android only offers "all the time" once normal location access is granted.
    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            step = ConfigStep.SETTINGS
        }
    }

    when (step) {
        ConfigStep.EXPLAIN -> ExplainSheet(
            onContinue = {
                if (hasForegroundLocation()) {
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    foregroundLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    )
                }
            },
            onNotNow = { step = ConfigStep.SETTINGS },
        )
        ConfigStep.SETTINGS -> opacity?.let { current ->
            SettingsSheet(
                opacity = current,
                onOpacityChange = { opacity = it },
                preview = { WidgetPreview(snapshot ?: SampleData.dayPartlyCloudy, small, current) },
                onDone = { onDone(current) },
            )
        }
    }
}

@Composable
private fun ExplainSheet(onContinue: () -> Unit, onNotNow: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(width = 32.dp, height = 4.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(Modifier.height(16.dp))
                Icon(
                    Icons.Rounded.ShareLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "Keep the widget up to date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                Text(
                    buildAnnotatedString {
                        append("So the widget shows your current area even when Weather is closed, choose ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Allow all the time") }
                        append(" on the next screen.")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Continue") }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Not now") }
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    opacity: Int,
    onOpacityChange: (Int) -> Unit,
    preview: @Composable () -> Unit,
    onDone: () -> Unit,
) {
    // No scrim: the activity is translucent, so the preview sits on the real wallpaper.
    Column(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        preview()
        Spacer(Modifier.height(18.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp)) {
                Text("Widget settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
                ) {
                    Text(
                        "You can change this later: long-press the widget and tap ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Opacity, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Background opacity", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("$opacity%", style = MaterialTheme.typography.titleSmall)
                }
                Slider(
                    value = opacity.toFloat(),
                    onValueChange = { onOpacityChange(it.toInt().coerceIn(0, 100)) },
                    valueRange = 0f..100f,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                    PRESETS.forEach { preset ->
                        PresetPill(preset, selected = preset == opacity, onClick = { onOpacityChange(preset) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDone) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PresetPill(value: Int, selected: Boolean, onClick: () -> Unit) {
    val colours = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.weight(1f).clip(CircleShape)
            .background(if (selected) colours.primary else colours.primaryContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$value%",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colours.onPrimary else colours.onPrimaryContainer,
        )
    }
}

/** A Compose copy of the widget, so the opacity can be judged against the real wallpaper. */
@Composable
private fun WidgetPreview(snapshot: WeatherSnapshot, small: Boolean, opacityPct: Int) {
    val look = widgetTextStyle(opacityPct)
    val scheme = MaterialTheme.colorScheme
    val white = look.whiteWithShadow
    val text = if (white) Color.White else scheme.onSurface
    val subtle = if (white) Color.White else scheme.onSurfaceVariant
    val shadow = if (white) Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 8f) else null
    val pillBg = if (white) Color.Black.copy(alpha = 0.25f) else scheme.primaryContainer
    val pillFg = if (white) Color.White else scheme.onPrimaryContainer
    val now = snapshot.now
    val icon = weatherIconRes(now.condition, now.isDay)
    val label = conditionLabel(now.condition, now.isDay)
    fun style(size: Int, weight: FontWeight = FontWeight.Normal, colour: Color = text) =
        TextStyle(color = colour, fontSize = size.sp, fontWeight = weight, shadow = shadow)

    val pill = @Composable {
        Text(
            "Feels ${formatDegrees(now.feelsLikeC)}",
            style = style(11, FontWeight.SemiBold, pillFg).copy(shadow = null),
            modifier = Modifier.clip(CircleShape).background(pillBg).padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
    val card = Modifier.clip(RoundedCornerShape(26.dp))
        .background(scheme.surface.copy(alpha = look.backgroundAlpha))

    if (small) {
        Column(card.size(140.dp).padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(snapshot.place.areaName, style = style(11, FontWeight.SemiBold, subtle))
            Column {
                Text(formatDegrees(now.temperatureC), style = style(40))
                pill()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                    androidx.compose.foundation.Image(painterResource(icon), null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = style(11))
                }
            }
        }
    } else {
        val time = LocalDateTime.now()
        Column(card.fillMaxWidth().height(150.dp).padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(snapshot.place.areaName, style = style(11, FontWeight.SemiBold, subtle), modifier = Modifier.weight(1f))
                Text(
                    "$label · H${formatDegrees(snapshot.todayHighC)} L${formatDegrees(snapshot.todayLowC)}",
                    style = style(11, FontWeight.SemiBold),
                    textAlign = TextAlign.End,
                )
            }
            Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), style = style(38, FontWeight.Medium))
                    Text(
                        time.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)),
                        style = style(12, FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val iconModifier = if (white) {
                            Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)).padding(3.dp)
                        } else {
                            Modifier
                        }
                        Box(iconModifier) {
                            androidx.compose.foundation.Image(painterResource(icon), label, Modifier.size(30.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(formatDegrees(now.temperatureC), style = style(34))
                    }
                    Spacer(Modifier.height(4.dp))
                    pill()
                }
            }
        }
    }
}

/** Material 3 with the wallpaper colours, light or dark like the system. */
@Composable
private fun ConfigTheme(content: @Composable () -> Unit) {
    val context: Context = LocalContext.current
    val scheme: ColorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}
