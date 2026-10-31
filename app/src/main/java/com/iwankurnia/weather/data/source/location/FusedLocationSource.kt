package com.iwankurnia.weather.data.source.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.iwankurnia.weather.data.GeoPoint
import com.iwankurnia.weather.data.LocationSource
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The phone's position from the fused location provider, at balanced (city-block) accuracy.
 *
 * Tries a fresh fix first and falls back to the last known one. Null when location
 * permission is missing, no fix is available, or Play services fails.
 */
class FusedLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext
    private val client by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    @OptIn(ExperimentalCoroutinesApi::class) // Task.await(CancellationTokenSource)
    @SuppressLint("MissingPermission") // Checked by hasPermission() just before.
    override suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        return try {
            val tokenSource = CancellationTokenSource()
            val fresh = withTimeoutOrNull(FIX_TIMEOUT_MS) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                    .await(tokenSource)
            }
            val location = fresh ?: client.lastLocation.await()
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
        }

    private companion object {
        const val FIX_TIMEOUT_MS = 15_000L
    }
}
