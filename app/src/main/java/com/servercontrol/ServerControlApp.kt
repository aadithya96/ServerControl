package com.servercontrol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ServerControlApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Server Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for CPU, disk and connectivity thresholds"
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITORING,
                "Background Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing background monitoring service"
            }

            manager.createNotificationChannels(listOf(alertChannel, monitorChannel))
        }
    }

    companion object {
        const val CHANNEL_ALERTS = "server_alerts"
        const val CHANNEL_MONITORING = "background_monitoring"
    }
}
