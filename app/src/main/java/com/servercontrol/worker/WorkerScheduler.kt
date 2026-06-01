package com.servercontrol.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkerScheduler {

    private const val WORK_NAME = "server_monitor"
    private const val METRICS_WORK_NAME = "metrics_sampler"
    private const val WIDGET_WORK_NAME = "widget_updater"

    // Android enforces a 15-minute minimum interval for periodic work; anything
    // smaller is silently clamped. Coerce explicitly so callers get predictable
    // behaviour.
    private const val MIN_INTERVAL_MINUTES = 15L
    private const val WIDGET_INTERVAL_MINUTES = 30L

    private val networkConstraints: Constraints
        get() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun schedule(context: Context, intervalMinutes: Long) {
        val request = PeriodicWorkRequestBuilder<ServerMonitorWorker>(
            intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES), TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun scheduleMetricsSampling(context: Context, intervalMinutes: Long = MIN_INTERVAL_MINUTES) {
        val request = PeriodicWorkRequestBuilder<MetricsSamplerWorker>(
            intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES), TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            METRICS_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelMetricsSampling(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(METRICS_WORK_NAME)
    }

    fun scheduleWidgetUpdater(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetUpdaterWorker>(
            WIDGET_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WIDGET_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelWidgetUpdater(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_WORK_NAME)
    }

    /** Convenience used at app startup and when background monitoring is enabled. */
    fun scheduleAll(context: Context, intervalMinutes: Long) {
        schedule(context, intervalMinutes)
        scheduleMetricsSampling(context, intervalMinutes)
        scheduleWidgetUpdater(context)
    }

    /** Convenience used when background monitoring is disabled. */
    fun cancelAll(context: Context) {
        cancel(context)
        cancelMetricsSampling(context)
        cancelWidgetUpdater(context)
    }
}
