package com.iwankurnia.weather.ui.main

import com.iwankurnia.weather.model.WeatherSnapshot
import java.time.Instant

/** Everything the main screen can show. */
sealed interface MainUiState {
    /** First fetch in progress, nothing to show yet. */
    data object Loading : MainUiState

    /** First launch: explain why location is needed before asking for it. */
    data object Welcome : MainUiState

    /** Location permission was refused. */
    data object LocationDenied : MainUiState

    /** Location is allowed but there is no fix, for example with location services off. */
    data object LocationUnavailable : MainUiState

    /** The first fetch failed and nothing is cached. */
    data class Error(val message: String) : MainUiState

    /** Weather to show; [staleSince] is set when it is an old snapshot shown offline. */
    data class Ready(val snapshot: WeatherSnapshot, val staleSince: Instant?) : MainUiState
}
