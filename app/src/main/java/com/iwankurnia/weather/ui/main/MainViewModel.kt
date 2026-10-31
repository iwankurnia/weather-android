package com.iwankurnia.weather.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwankurnia.weather.data.LoadResult
import com.iwankurnia.weather.data.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Turns [WeatherRepository] results into [MainUiState].
 *
 * Nothing loads until [refresh] is called, so the caller can first decide between
 * [showWelcome] (no location permission yet) and a normal load.
 */
class MainViewModel(private val repo: WeatherRepository) : ViewModel() {
    private val _state = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)

    /** True while a refresh runs on top of weather already on screen (pull to refresh). */
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var job: Job? = null

    /** Loads the weather; [force] skips any freshness shortcuts in the repository. */
    fun refresh(force: Boolean) {
        if (job?.isActive == true) return
        val shown = _state.value as? MainUiState.Ready
        if (shown != null) _refreshing.value = true else _state.value = MainUiState.Loading
        job = viewModelScope.launch {
            val result = try {
                repo.load(force)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Failed(e.message ?: "Something went wrong")
            }
            _state.value = map(result, shown)
            _refreshing.value = false
        }
    }

    /** Shows the first-launch explanation (location permission not asked yet). */
    fun showWelcome() {
        _state.value = MainUiState.Welcome
    }

    /** Shows the location-refused screen after the permission dialog was declined. */
    fun showLocationDenied() {
        _state.value = MainUiState.LocationDenied
    }

    private fun map(result: LoadResult, shown: MainUiState.Ready?): MainUiState = when (result) {
        is LoadResult.Fresh -> MainUiState.Ready(result.snapshot, staleSince = null)
        is LoadResult.Stale -> MainUiState.Ready(result.snapshot, staleSince = result.snapshot.fetchedAt)
        is LoadResult.NoLocation ->
            if (result.permissionDenied) MainUiState.LocationDenied else MainUiState.Welcome
        // Never blank the screen: keep what is shown and mark it as old.
        is LoadResult.Failed ->
            shown?.let { MainUiState.Ready(it.snapshot, staleSince = it.snapshot.fetchedAt) }
                ?: MainUiState.Error(result.message)
    }
}
