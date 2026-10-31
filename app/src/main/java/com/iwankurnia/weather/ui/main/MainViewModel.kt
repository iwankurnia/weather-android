package com.iwankurnia.weather.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwankurnia.weather.data.LoadResult
import com.iwankurnia.weather.data.WeatherRepository
import com.iwankurnia.weather.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Turns [WeatherRepository] results into [MainUiState].
 *
 * Nothing loads until [start] (or [refresh]) is called, so the caller can first decide between
 * [showWelcome] (no location permission yet) and a normal load.
 */
class MainViewModel(private val repo: WeatherRepository) : ViewModel() {
    private val _state = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)

    /** True while a refresh runs on top of weather already on screen (pull to refresh). */
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var job: Job? = null
    private var started = false

    /**
     * Runs the launch flow once per ViewModel: a no-op after rotation, but runs again after
     * process death, when the activity gets a fresh ViewModel.
     */
    fun start(hasLocation: Boolean, locationAsked: Boolean) {
        if (started) return
        started = true
        when {
            hasLocation -> refresh(force = false)
            locationAsked -> showLocationDenied()
            else -> showWelcome()
        }
    }

    /**
     * Loads the weather; [force] skips any freshness shortcuts in the repository. With no weather
     * on screen, the cached snapshot (if any) is shown first while refreshing; it is only marked
     * as old (offline banner) if the refresh then fails.
     */
    fun refresh(force: Boolean) {
        if (job?.isActive == true) return
        val before = _state.value
        val shown = before as? MainUiState.Ready
        if (shown != null) _refreshing.value = true else _state.value = MainUiState.Loading
        // From a location screen, keep the spinner rather than flash weather it may take away.
        val showCache = shown == null &&
            before !is MainUiState.LocationDenied && before !is MainUiState.LocationUnavailable
        job = viewModelScope.launch {
            var base = shown
            if (showCache) {
                base = cachedSnapshot()?.let { MainUiState.Ready(it, staleSince = null) }
                if (base != null) {
                    _state.value = base
                    _refreshing.value = true
                }
            }
            val result = try {
                repo.load(force)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LoadResult.Failed(e.message ?: "Something went wrong")
            }
            _state.value = map(result, base)
            _refreshing.value = false
        }
    }

    private suspend fun cachedSnapshot(): WeatherSnapshot? = try {
        repo.cached()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
        // Not denied means granted but no fix, for example with location services off.
        is LoadResult.NoLocation ->
            if (result.permissionDenied) MainUiState.LocationDenied else MainUiState.LocationUnavailable
        // Never blank the screen: keep what is shown and mark it as old.
        is LoadResult.Failed ->
            shown?.let { MainUiState.Ready(it.snapshot, staleSince = it.snapshot.fetchedAt) }
                ?: MainUiState.Error(result.message)
    }
}
