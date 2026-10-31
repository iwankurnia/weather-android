package com.iwankurnia.weather.data.source.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.iwankurnia.weather.data.AreaNamer
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Area name for a point from the platform geocoder: the sub-locality, else the locality.
 *
 * Uses the listener form on API 33+ and the blocking call on [Dispatchers.IO] on 31–32.
 * Null when no geocoder is present, nothing is found, or anything fails.
 */
class GeocoderAreaNamer(context: Context) : AreaNamer {
    private val appContext = context.applicationContext

    override suspend fun name(lat: Double, lon: Double): String? = try {
        if (!Geocoder.isPresent()) {
            null
        } else {
            val geocoder = Geocoder(appContext, Locale.getDefault())
            val address = withTimeoutOrNull(LOOKUP_TIMEOUT_MS) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lookup(geocoder, lat, lon)
                } else {
                    withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                    }
                }
            }
            address?.let { it.subLocality ?: it.locality }?.trim()?.takeIf { it.isNotEmpty() }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun lookup(geocoder: Geocoder, lat: Double, lon: Double): Address? =
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(
                lat,
                lon,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(null)
                    }
                },
            )
        }

    private companion object {
        const val LOOKUP_TIMEOUT_MS = 10_000L
    }
}
