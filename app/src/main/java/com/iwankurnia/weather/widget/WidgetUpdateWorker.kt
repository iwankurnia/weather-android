package com.iwankurnia.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iwankurnia.weather.data.weatherRepositoryProvider
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Background refresh for the widgets: fetches a fresh snapshot (BMKG still throttled to once
 * an hour), then redraws every widget. Without background location access the repository falls
 * back to the cached place.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            applicationContext.weatherRepositoryProvider()?.weatherRepository?.load(forceRefresh = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The widgets keep showing the cached snapshot; the next run tries again.
        }
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }
}

object WidgetUpdater {
    const val WORK_NAME = "widget-refresh"

    private val _refreshes = MutableStateFlow(0)

    /** Bumped on every [updateAll], so widget sessions that are still running reload the cache. */
    val refreshes: StateFlow<Int> = _refreshes.asStateFlow()

    const val REFRESH_NOW_WORK_NAME = "widget-refresh-now"

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Refresh about every 30 minutes while online. Safe to call repeatedly (KEEP). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(online)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * One refresh as soon as the phone is online, so a new widget, or one that has just been
     * given location access, need not wait for the next periodic run. A newer call replaces one
     * still waiting or running (REPLACE).
     */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().setConstraints(online).build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(REFRESH_NOW_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Stops the refresh once no widget is on the home screen. */
    fun cancelIfUnused(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val inUse = WIDGET_RECEIVERS.any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
        if (!inUse) WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Redraws every widget, of every kind, from the cache. */
    suspend fun updateAll(context: Context) {
        _refreshes.update { it + 1 }
        LargeWidget().updateAll(context)
        SmallWidget().updateAll(context)
        StripWidget().updateAll(context)
        SceneWidget().updateAll(context)
    }

    /** True while any of the app's widgets is on a home screen. */
    fun anyInUse(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return WIDGET_RECEIVERS.any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
    }
}

/** Every widget the app offers; keep in step with the receivers in the manifest. */
internal val WIDGET_RECEIVERS = listOf(
    LargeWidgetReceiver::class.java,
    SmallWidgetReceiver::class.java,
    StripWidgetReceiver::class.java,
    SceneWidgetReceiver::class.java,
)
