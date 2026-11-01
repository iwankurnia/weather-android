package com.iwankurnia.weather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.iwankurnia.weather.ui.main.MainUiState
import com.iwankurnia.weather.ui.main.MainViewModel
import com.iwankurnia.weather.ui.main.WeatherRoute
import com.iwankurnia.weather.widget.WidgetUpdater
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Hosts the main screen. Location flow: first launch shows the welcome screen; "Continue" asks for
 * location; a refusal shows the location-refused screen, which recovers when the user returns
 * from settings with access granted. With access but no fix (location turned off), the
 * can't-find-you screen links to the location settings and retries on return.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel((application as WeatherApplication).weatherRepository) as T
        }
    }

    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        markLocationAsked()
        if (results.values.any { it }) viewModel.refresh(force = true) else viewModel.showLocationDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Not tied to savedInstanceState: after process death the fresh ViewModel must start too.
        viewModel.start(hasLocation = hasLocation(), locationAsked = locationAsked())
        if (hasAnyWidget()) WidgetUpdater.schedule(applicationContext)

        // Every fresh snapshot is also saved to the cache, so redraw the widgets from it.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.filterIsInstance<MainUiState.Ready>().distinctUntilChanged().collect {
                    if (hasAnyWidget()) WidgetUpdater.updateAll(applicationContext)
                }
            }
        }

        setContent {
            WeatherRoute(
                viewModel = viewModel,
                onRequestLocation = {
                    askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                },
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                },
                onOpenLocationSettings = {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from settings with access now granted, or with location now turned on.
        val state = viewModel.state.value
        val retry = state is MainUiState.LocationUnavailable ||
            (hasLocation() && (state is MainUiState.LocationDenied || state is MainUiState.Welcome))
        if (retry) viewModel.refresh(force = true)
    }

    private fun hasLocation() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun locationAsked() = getPreferences(MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    private fun markLocationAsked() = getPreferences(MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }

    private fun hasAnyWidget(): Boolean = WidgetUpdater.anyInUse(this)

    private companion object {
        const val KEY_ASKED = "location_asked"
    }
}
