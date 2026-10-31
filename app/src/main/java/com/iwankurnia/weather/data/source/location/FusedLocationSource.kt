package com.iwankurnia.weather.data.source.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
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
 * Tries a network fix, then a GPS fix, then the last known one. Null when location
 * permission is missing, no fix is available, or Play services fails.
 */
class FusedLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext
    private val client by lazy { LocationServices.getFusedLocationProviderClient(appContext) }

    @SuppressLint("MissingPermission") // Checked by hasPermission() just before.
    override suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        return try {
            // Wi-Fi and cell first (cheap); GPS only if they give nothing, e.g. weak network location.
            val location = fix(Priority.PRIORITY_BALANCED_POWER_ACCURACY, BALANCED_TIMEOUT_MS)
                ?: fix(Priority.PRIORITY_HIGH_ACCURACY, GPS_TIMEOUT_MS)
                ?: client.lastLocation.await()
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    /** One fix at [priority]; a fix up to [MAX_FIX_AGE_MS] old comes back at once. Null on timeout. */
    @OptIn(ExperimentalCoroutinesApi::class) // Task.await(CancellationTokenSource)
    @SuppressLint("MissingPermission") // Checked by hasPermission() in current().
    private suspend fun fix(priority: Int, timeoutMs: Long): Location? {
        val tokenSource = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(MAX_FIX_AGE_MS)
            .setDurationMillis(timeoutMs)
            .build()
        return withTimeoutOrNull(timeoutMs) { client.getCurrentLocation(request, tokenSource.token).await(tokenSource) }
    }

    private fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
        }

    private companion object {
        const val BALANCED_TIMEOUT_MS = 5_000L
        const val GPS_TIMEOUT_MS = 6_000L
        const val MAX_FIX_AGE_MS = 10 * 60_000L
    }
}
