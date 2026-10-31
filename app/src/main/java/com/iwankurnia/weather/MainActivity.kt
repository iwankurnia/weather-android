package com.iwankurnia.weather

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import com.iwankurnia.weather.widget.LargeWidgetReceiver
import com.iwankurnia.weather.widget.SmallWidgetReceiver
import com.iwankurnia.weather.widget.WidgetUpdater
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Hosts the main screen. Location flow: first launch shows the welcome screen; "Continue" asks for
 * location; a refusal shows the location-refused screen, which recovers when the user returns
 * from settings with access granted.
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
        if (savedInstanceState == null) startFlow()
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
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from settings with access now granted.
        val state = viewModel.state.value
        if (hasLocation() && (state is MainUiState.LocationDenied || state is MainUiState.Welcome)) {
            viewModel.refresh(force = true)
        }
    }

    private fun startFlow() = when {
        hasLocation() -> viewModel.refresh(force = false)
        locationAsked() -> viewModel.showLocationDenied()
        else -> viewModel.showWelcome()
    }

    private fun hasLocation() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun locationAsked() = getPreferences(MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    private fun markLocationAsked() = getPreferences(MODE_PRIVATE).edit { putBoolean(KEY_ASKED, true) }

    private fun hasAnyWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(this)
        return listOf(LargeWidgetReceiver::class.java, SmallWidgetReceiver::class.java)
            .any { manager.getAppWidgetIds(ComponentName(this, it)).isNotEmpty() }
    }

    private companion object {
        const val KEY_ASKED = "location_asked"
    }
}
