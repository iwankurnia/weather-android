package com.iwankurnia.weather.ui.background

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * False when the system "Remove animations" setting is on (`ANIMATOR_DURATION_SCALE` is 0).
 * Follows the setting live, so turning it on stops the backgrounds without a restart.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    if (LocalInspectionMode.current) return true
    val resolver = LocalContext.current.contentResolver
    var enabled by remember(resolver) { mutableStateOf(readAnimationsEnabled(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = readAnimationsEnabled(resolver)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val registered = runCatching { resolver.registerContentObserver(uri, false, observer) }.isSuccess
        onDispose {
            if (registered) resolver.unregisterContentObserver(observer)
        }
    }
    return enabled
}

private fun readAnimationsEnabled(resolver: ContentResolver): Boolean = try {
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
} catch (_: SecurityException) {
    true
}
