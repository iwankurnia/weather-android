package com.iwankurnia.weather.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iwankurnia.weather.model.SampleData
import com.iwankurnia.weather.model.WeatherSnapshot
import com.iwankurnia.weather.ui.background.WeatherBackground
import com.iwankurnia.weather.ui.background.cardTint
import com.iwankurnia.weather.ui.theme.WeatherText
import com.iwankurnia.weather.ui.theme.WeatherTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The app's only screen: renders [MainViewModel.state] inside [WeatherTheme].
 * Draw it edge to edge; it pads itself for the status and navigation bars.
 */
@Composable
fun WeatherRoute(viewModel: MainViewModel, onRequestLocation: () -> Unit, onOpenSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    WeatherTheme {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it::class },
            label = "state",
        ) { shown ->
            when (shown) {
                MainUiState.Loading -> LoadingScreen()
                MainUiState.Welcome -> WelcomeScreen(onContinue = onRequestLocation)
                MainUiState.LocationDenied -> LocationDeniedScreen(
                    onOpenSettings = onOpenSettings,
                    onTryAgain = { viewModel.refresh(true) },
                )
                is MainUiState.Error -> ServiceErrorScreen(onTryAgain = { viewModel.refresh(true) })
                is MainUiState.Ready -> MainScreen(
                    snapshot = shown.snapshot,
                    staleSince = shown.staleSince,
                    refreshing = refreshing,
                    onRefresh = { viewModel.refresh(true) },
                )
            }
        }
    }
}

/**
 * The weather itself: one long scrolling column over the weather background, with pull to
 * refresh. [nowTime], [today] and [zone] are parameters so previews stay fixed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    snapshot: WeatherSnapshot,
    staleSince: Instant?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    nowTime: LocalTime = LocalTime.now(),
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val now = snapshot.now
    val colours = cardTint(now.condition, now.isDay)
    val pullState = rememberPullToRefreshState()
    Box(modifier.fillMaxSize()) {
        WeatherBackground(now.condition, now.isDay, Modifier.fillMaxSize())
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    containerColor = colours.surface,
                    color = colours.primary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
            },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 18.dp),
            ) {
                TopBar(
                    label = snapshot.place.areaName,
                    icon = Icons.Rounded.LocationOn,
                    isDay = now.isDay,
                    contentColour = colours.onSky,
                    onRefresh = onRefresh,
                )
                if (staleSince != null) OfflineBanner(staleSince, zone = zone)
                Hero(now, snapshot.todayHighC, snapshot.todayLowC, colours)
                ForecastCard(snapshot.summary, now, snapshot.steps, colours)
                Spacer(Modifier.height(8.dp))
                DailyList(snapshot.days, today, colours)
                Spacer(Modifier.height(8.dp))
                DetailTiles(snapshot, nowTime, colours)
                Spacer(Modifier.height(14.dp))
                CreditsLine(snapshot, colours, zone = zone)
            }
        }
    }
}

/**
 * Location chip and round refresh button. With [onRefresh] null the button is shown but
 * inactive (first load).
 */
@Composable
internal fun TopBar(
    label: String,
    icon: ImageVector,
    isDay: Boolean,
    contentColour: Color,
    onRefresh: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val chipBackground = Color.White.copy(alpha = if (isDay) 0.2f else 0.1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(percent = 50))
                .background(chipBackground)
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = contentColour, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = WeatherText.chip,
                color = contentColour,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(chipBackground)
                .then(
                    if (onRefresh != null) {
                        Modifier.clickable(onClickLabel = "Refresh", role = Role.Button, onClick = onRefresh)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh",
                tint = contentColour.copy(alpha = if (onRefresh != null) 1f else 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(name = "Day, partly cloudy", widthDp = 400, heightDp = 1900)
@Composable
private fun MainScreenDayPreview() = WeatherTheme {
    MainScreen(
        snapshot = SampleData.dayPartlyCloudy,
        staleSince = null,
        refreshing = false,
        onRefresh = {},
        nowTime = LocalTime.of(12, 41),
        today = LocalDate.of(2026, 11, 1),
        zone = ZoneId.of("Asia/Jakarta"),
    )
}

@Preview(name = "Night, rain", widthDp = 400, heightDp = 1900)
@Composable
private fun MainScreenNightPreview() = WeatherTheme {
    MainScreen(
        snapshot = SampleData.nightRain,
        staleSince = null,
        refreshing = false,
        onRefresh = {},
        nowTime = LocalTime.of(21, 15),
        today = LocalDate.of(2026, 11, 1),
        zone = ZoneId.of("Asia/Jakarta"),
    )
}

@Preview(name = "Offline", widthDp = 400, heightDp = 900)
@Composable
private fun MainScreenOfflinePreview() = WeatherTheme {
    MainScreen(
        snapshot = SampleData.dayPartlyCloudy,
        staleSince = SampleData.dayPartlyCloudy.fetchedAt,
        refreshing = false,
        onRefresh = {},
        nowTime = LocalTime.of(12, 41),
        today = LocalDate.of(2026, 11, 1),
        zone = ZoneId.of("Asia/Jakarta"),
    )
}
