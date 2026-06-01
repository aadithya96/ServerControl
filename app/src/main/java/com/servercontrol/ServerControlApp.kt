package com.servercontrol

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.servercontrol.presentation.settings.SettingsKeys
import com.servercontrol.util.NotificationHelper
import com.servercontrol.worker.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ServerControlApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        restoreWorkers()
    }

    /**
     * Re-enqueue background workers based on persisted settings. Periodic work is
     * enqueued with KEEP/UPDATE policies, so calling this on every process start
     * is idempotent and safe.
     */
    private fun restoreWorkers() {
        appScope.launch {
            val prefs = dataStore.data.first()
            val bgEnabled = prefs[SettingsKeys.BG_MONITORING] ?: false
            val intervalMinutes = (prefs[SettingsKeys.BG_INTERVAL] ?: 15).toLong()
            if (bgEnabled) {
                WorkerScheduler.scheduleAll(this@ServerControlApp, intervalMinutes)
            } else {
                // Widgets still refresh even when full background monitoring is off.
                WorkerScheduler.scheduleWidgetUpdater(this@ServerControlApp)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
