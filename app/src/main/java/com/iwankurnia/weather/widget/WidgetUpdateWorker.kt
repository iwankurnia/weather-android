package com.iwankurnia.weather.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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

    /** Refresh about every 30 minutes while online. Safe to call repeatedly (KEEP). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Stops the refresh once neither widget is on the home screen. */
    fun cancelIfUnused(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val inUse = listOf(LargeWidgetReceiver::class.java, SmallWidgetReceiver::class.java).any {
            manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
        }
        if (!inUse) WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Redraws every large and small widget from the cache. */
    suspend fun updateAll(context: Context) {
        _refreshes.update { it + 1 }
        LargeWidget().updateAll(context)
        SmallWidget().updateAll(context)
    }
}
