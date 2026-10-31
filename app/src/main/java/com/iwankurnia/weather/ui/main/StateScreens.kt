package com.iwankurnia.weather.ui.main

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iwankurnia.weather.R
import com.iwankurnia.weather.model.Condition
import com.iwankurnia.weather.ui.theme.WeatherShapes
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** First launch: why the app needs location. "Continue" asks for the permission. */
@Composable
fun WelcomeScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    PlainScreen(
        modifier = modifier,
        badge = {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF5AA9E6), Color(0xFF2F6DB3)))),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.wx_partly_day), contentDescription = null, Modifier.size(64.dp))
            }
        },
        title = "Weather where you are",
        text = "To show the weather for your current area, Weather needs your location. " +
            "It is only used to fetch the forecast and never leaves your phone for anything else.",
    ) {
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), contentPadding = ButtonPadding) {
            Text("Continue", style = WeatherText.button)
        }
    }
}

/** Location permission was refused: offer the app's settings page or another try. */
@Composable
fun LocationDeniedScreen(onOpenSettings: () -> Unit, onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    PlainScreen(
        modifier = modifier,
        badge = { EmptyBadge(Icons.Rounded.LocationOff) },
        title = "Location is off for Weather",
        text = "Weather can’t tell where you are, so there’s nothing to show yet.",
    ) {
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(), contentPadding = ButtonPadding) {
            Text("Open settings", style = WeatherText.button)
        }
        FilledTonalButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth(), contentPadding = ButtonPadding) {
            Text("Try again", style = WeatherText.button)
        }
    }
}

/** The first fetch failed and nothing is saved yet. */
@Composable
fun ServiceErrorScreen(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    PlainScreen(
        modifier = modifier,
        badge = { EmptyBadge(Icons.Rounded.SyncProblem) },
        title = "Couldn’t get the weather",
        text = "The weather service didn’t answer. This is usually brief.",
    ) {
        Button(onClick = onTryAgain, modifier = Modifier.fillMaxWidth(), contentPadding = ButtonPadding) {
            Text("Try again", style = WeatherText.button)
        }
    }
}

/** First load: a still partly-cloudy sky, a spinning cookie and placeholder cards. */
@Composable
fun LoadingScreen(modifier: Modifier = Modifier, isDay: Boolean = LocalTime.now().hour in 6..17) {
    val colours = cardTint(Condition.PARTLY_CLOUDY, isDay)
    Box(modifier.fillMaxSize()) {
        WeatherBackground(Condition.PARTLY_CLOUDY, isDay, Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp),
        ) {
            TopBar(
                label = "Finding you…",
                icon = Icons.Rounded.LocationSearching,
                isDay = isDay,
                contentColour = colours.onSky,
                onRefresh = null,
            )
            Spacer(Modifier.height(110.dp))
            CookieLoader(
                colour = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(72.dp),
            )
            Text(
                text = "Getting the weather…",
                style = WeatherText.stateBody,
                color = colours.onSky,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 18.dp),
            )
            Spacer(Modifier.height(70.dp))
            Skeleton(110)
            Skeleton(56)
            Skeleton(56)
        }
    }
}

@Composable
private fun Skeleton(heightDp: Int) {
    Box(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.25f)),
    )
}

/**
 * A spinning, gently breathing cookie: stands in for Material 3 `LoadingIndicator`, which the
 * resolved material3 (1.4.0) does not expose. Still when the system removes animations.
 */
@Composable
internal fun CookieLoader(colour: Color, modifier: Modifier = Modifier) {
    val animate = animationsEnabled()
    val transition = rememberInfiniteTransition(label = "loader")
    val turn = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "turn",
    )
    val breath = transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "breath",
    )
    Box(
        modifier
            .semantics { contentDescription = "Loading" }
            .graphicsLayer {
                if (animate) {
                    rotationZ = turn.value
                    scaleX = breath.value
                    scaleY = breath.value
                }
            }
            .background(colour, WeatherShapes.Cookie9Sided),
    )
}

/** False when "Remove animations" is on (animator duration scale 0). */
@Composable
private fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** "No connection. Showing weather from 10:12." above the hero when the weather is old. */
@Composable
internal fun OfflineBanner(since: Instant, modifier: Modifier = Modifier, zone: ZoneId = ZoneId.systemDefault()) {
    val ink = Color(0xFF3B2F00)
    Row(
        modifier = modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF4D6))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("No connection") }
                append("\nShowing weather from ${formatClock(since, zone)}.")
            },
            style = WeatherText.banner,
            color = ink,
        )
    }
}

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)

/** Shared layout of the plain screens: badge, title and text centred, buttons at the bottom. */
@Composable
private fun PlainScreen(
    badge: @Composable () -> Unit,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxSize()
            .background(scheme.surface)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 80.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            badge()
            Text(
                text = title,
                style = WeatherText.stateTitle,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            Text(text, style = WeatherText.stateBody, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = buttons)
    }
}

/** An icon in a primary-container cookie, for the empty states. */
@Composable
private fun EmptyBadge(icon: ImageVector) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(96.dp)
            .background(scheme.primaryContainer, WeatherShapes.Cookie9Sided),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(44.dp))
    }
}

@Preview(widthDp = 380, heightDp = 780)
@Composable
private fun WelcomePreview() = WeatherTheme(darkTheme = false) { WelcomeScreen(onContinue = {}) }

@Preview(widthDp = 380, heightDp = 780)
@Composable
private fun LocationDeniedPreview() = WeatherTheme(darkTheme = false) {
    LocationDeniedScreen(onOpenSettings = {}, onTryAgain = {})
}

@Preview(widthDp = 380, heightDp = 780)
@Composable
private fun ServiceErrorPreview() = WeatherTheme(darkTheme = false) { ServiceErrorScreen(onTryAgain = {}) }

@Preview(widthDp = 380, heightDp = 780)
@Composable
private fun LoadingPreview() = WeatherTheme { LoadingScreen(isDay = true) }

@Preview(widthDp = 380)
@Composable
private fun OfflineBannerPreview() = WeatherTheme {
    OfflineBanner(Instant.parse("2026-11-01T03:12:00Z"), zone = ZoneId.of("Asia/Jakarta"))
}
