package com.iwankurnia.weather.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text

/** Placeholder; replaced by plan Task 6. */
class SmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = object : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideContent { Text("Weather") }
        }
    }
}
