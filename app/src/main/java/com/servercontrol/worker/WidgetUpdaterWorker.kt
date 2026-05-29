package com.servercontrol.worker

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.util.Resource
import com.servercontrol.widget.ServerMetricsWidget
import com.servercontrol.widget.ServerStatusWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetUpdaterWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val servers = serverRepository.getAllServers()
            if (servers.isEmpty()) return Result.success()

            // Use the first server as default widget server (or could be configurable per widget)
            val server = servers.first()

            val stats = if (server.authType == AuthType.AGENT_TOKEN) {
                when (val r = agentDataSource.getStats(server)) {
                    is Resource.Success -> r.data
                    else -> null
                }
            } else {
                sshDataSource.getSystemStats(server).getOrNull()?.let { s ->
                    com.servercontrol.data.remote.dto.SystemStatsDto(
                        hostname = s.hostname,
                        uptimeSeconds = s.uptimeSeconds,
                        loadAvg1 = s.loadAvg1m,
                        loadAvg5 = s.loadAvg5m,
                        loadAvg15 = s.loadAvg15m,
                        cpuPercent = s.cpuPercent,
                        cpuCores = s.cpuCores,
                        memTotalBytes = s.memTotalBytes,
                        memUsedBytes = s.memUsedBytes,
                        memFreeBytes = s.memFreeBytes,
                        swapTotalBytes = s.swapTotalBytes,
                        swapUsedBytes = s.swapUsedBytes
                    )
                }
            }

            val diskPercent = if (server.authType == AuthType.AGENT_TOKEN) {
                when (val r = agentDataSource.getDiskInfo(server)) {
                    is Resource.Success -> r.data.mounts.maxOfOrNull { it.usedPercent }?.toFloat() ?: 0f
                    else -> 0f
                }
            } else {
                sshDataSource.getDiskInfo(server).getOrNull()
                    ?.maxOfOrNull { it.usedPercent }?.toFloat() ?: 0f
            }

            val isOnline = stats != null
            val cpuPercent = stats?.cpuPercent?.toFloat() ?: 0f
            val ramPercent = if ((stats?.memTotalBytes ?: 0L) > 0L) {
                ((stats?.memUsedBytes ?: 0L).toFloat() / (stats?.memTotalBytes ?: 1L).toFloat() * 100f)
            } else 0f

            val glanceManager = GlanceAppWidgetManager(applicationContext)

            // Update ServerStatusWidget instances
            val statusIds = glanceManager.getGlanceIds(ServerStatusWidget::class.java)
            for (id in statusIds) {
                updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[stringPreferencesKey("widget_server_name")] = server.name
                        this[floatPreferencesKey("widget_cpu_percent")] = cpuPercent
                        this[androidx.datastore.preferences.core.booleanPreferencesKey("widget_is_online")] = isOnline
                    }
                }
                ServerStatusWidget().update(applicationContext, id)
            }

            // Update ServerMetricsWidget instances
            val metricsIds = glanceManager.getGlanceIds(ServerMetricsWidget::class.java)
            for (id in metricsIds) {
                updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[stringPreferencesKey("widget_server_name")] = server.name
                        this[floatPreferencesKey("widget_cpu_percent")] = cpuPercent
                        this[floatPreferencesKey("widget_ram_percent")] = ramPercent
                        this[floatPreferencesKey("widget_disk_percent")] = diskPercent
                    }
                }
                ServerMetricsWidget().update(applicationContext, id)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
