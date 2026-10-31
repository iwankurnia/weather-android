package com.iwankurnia.weather.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore by preferencesDataStore(name = "widget_prefs")

/** Per-widget settings, keyed by app widget id. Opacity is 0–100 and defaults to 100. */
object WidgetPrefs {
    const val DEFAULT_OPACITY = 100

    private fun opacityKey(appWidgetId: Int) = intPreferencesKey("opacity_$appWidgetId")

    fun opacityFlow(context: Context, appWidgetId: Int): Flow<Int> =
        context.applicationContext.widgetDataStore.data
            .map { (it[opacityKey(appWidgetId)] ?: DEFAULT_OPACITY).coerceIn(0, 100) }
            .catch { emit(DEFAULT_OPACITY) }
            .distinctUntilChanged()

    suspend fun opacity(context: Context, appWidgetId: Int): Int = opacityFlow(context, appWidgetId).first()

    suspend fun setOpacity(context: Context, appWidgetId: Int, opacityPct: Int) {
        context.applicationContext.widgetDataStore.edit { it[opacityKey(appWidgetId)] = opacityPct.coerceIn(0, 100) }
    }

    suspend fun clear(context: Context, appWidgetIds: IntArray) {
        context.applicationContext.widgetDataStore.edit { prefs -> appWidgetIds.forEach { prefs.remove(opacityKey(it)) } }
    }
}
