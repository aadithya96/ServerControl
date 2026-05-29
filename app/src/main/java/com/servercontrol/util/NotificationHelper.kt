package com.servercontrol.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.servercontrol.R

object NotificationHelper {

    const val CHANNEL_ALERTS = "server_alerts"
    const val CHANNEL_SYNC = "background_sync"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Server Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for CPU, disk, and connectivity thresholds"
            }

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Background Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring status"
            }

            manager.createNotificationChannels(listOf(alertChannel, syncChannel))
        }
    }

    fun sendCpuAlert(context: Context, serverName: String, cpuPercent: Float) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("High CPU: $serverName")
            .setContentText("CPU usage is ${"%.1f".format(cpuPercent)}% — above threshold")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(("cpu_$serverName").hashCode(), notification)
    }

    fun sendDiskAlert(context: Context, serverName: String, mountPoint: String, usagePercent: Float) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Disk Full: $serverName")
            .setContentText("$mountPoint is ${"%.1f".format(usagePercent)}% full — above threshold")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(("disk_${serverName}_$mountPoint").hashCode(), notification)
    }

    fun sendUnreachableAlert(context: Context, serverName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Server Unreachable")
            .setContentText("$serverName is not responding")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(("unreachable_$serverName").hashCode(), notification)
    }
}
