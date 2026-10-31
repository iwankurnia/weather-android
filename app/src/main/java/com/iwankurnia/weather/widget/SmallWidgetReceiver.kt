package com.iwankurnia.weather.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdater.schedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        forgetWidgets(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdater.cancelIfUnused(context)
    }
}

private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Drops the settings of removed widgets. Glance already holds the receiver's `goAsync`, so this
 * runs fire-and-forget; a leftover key is harmless because widget ids are never reused.
 */
internal fun forgetWidgets(context: Context, appWidgetIds: IntArray) {
    val app = context.applicationContext
    cleanupScope.launch { runCatching { WidgetPrefs.clear(app, appWidgetIds) } }
}
