package com.iwankurnia.weather.data.source

import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** The one HTTP client the app uses, shared so connections and threads are pooled. */
val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
}

/** Fetches [url] as text. Throws [IOException] on a network failure or a non-2xx status. */
suspend fun getText(url: String): String = suspendCancellableCoroutine { cont ->
    val call = httpClient.newCall(Request.Builder().url(url).get().build())
    cont.invokeOnCancellation { call.cancel() }
    call.enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw IOException("HTTP ${it.code} for ${it.request.url.host}")
                        it.body?.string() ?: throw IOException("Empty body from ${it.request.url.host}")
                    }
                }
                result.fold({ cont.resume(it) }, { cont.resumeWithException(it) })
            }
        },
    )
}

/** Fetches a URL as text; injectable so the clients can be tested without the network. */
typealias TextFetcher = suspend (url: String) -> String

/** Coordinates for a URL: four decimals (about 11 m), never in exponent form. */
internal fun coord(value: Double): String = String.format(Locale.ROOT, "%.4f", value)

/** Like [runCatching], but lets coroutine cancellation through. */
internal inline fun <T> catchingOrNull(block: () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

// Shared helpers for the source parsers.

/** Shared parser settings: unknown keys are ignored so new API fields never break parsing. */
internal val sourceJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

internal fun JsonElement?.asArray(): JsonArray? = this as? JsonArray

/** The text of a primitive, or null for JSON null, objects and arrays. */
internal fun JsonElement?.text(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

/** A number, accepting numbers sent as strings ("27", " 3.4 "). */
internal fun JsonElement?.number(): Double? = text()?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() }

/** A whole number, accepting "27", 27 and 27.0. */
internal fun JsonElement?.integer(): Int? = number()?.let { Math.round(it).toInt() }
