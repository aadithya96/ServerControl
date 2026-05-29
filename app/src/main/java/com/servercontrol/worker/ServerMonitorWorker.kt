package com.servercontrol.worker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.dto.MountDto
import com.servercontrol.data.remote.dto.SystemStatsDto
import com.servercontrol.data.remote.dto.toDomain
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.util.NotificationHelper
import com.servercontrol.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ServerMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val dataStore: DataStore<Preferences>
) : CoroutineWorker(context, params) {

    private object Keys {
        val CPU_ALERT = intPreferencesKey("cpu_alert_threshold")
        val DISK_ALERT = intPreferencesKey("disk_alert_threshold")
    }

    override suspend fun doWork(): Result {
        return try {
            val prefs = dataStore.data.first()
            val cpuThreshold = prefs[Keys.CPU_ALERT] ?: 80
            val diskThreshold = prefs[Keys.DISK_ALERT] ?: 90

            val servers = serverRepository.getAllServers()

            for (server in servers) {
                try {
                    // Fetch system stats
                    val cpuPercent: Double? = if (server.authType == AuthType.AGENT_TOKEN) {
                        when (val r = agentDataSource.getStats(server)) {
                            is Resource.Success -> r.data.cpuPercent
                            else -> null
                        }
                    } else {
                        sshDataSource.getSystemStats(server).getOrNull()?.cpuPercent
                    }

                    if (cpuPercent == null) {
                        NotificationHelper.sendUnreachableAlert(applicationContext, server.name)
                        continue
                    }

                    if (cpuPercent > cpuThreshold) {
                        NotificationHelper.sendCpuAlert(
                            applicationContext,
                            server.name,
                            cpuPercent.toFloat()
                        )
                    }

                    // Fetch disk info
                    val diskMounts = if (server.authType == AuthType.AGENT_TOKEN) {
                        when (val r = agentDataSource.getDiskInfo(server)) {
                            is Resource.Success -> r.data.mounts.map { it.toDomain() }
                            else -> null
                        }
                    } else {
                        sshDataSource.getDiskInfo(server).getOrNull()
                    }

                    diskMounts
                        ?.filter { it.usedPercent > diskThreshold }
                        ?.forEach { mount ->
                            NotificationHelper.sendDiskAlert(
                                applicationContext,
                                server.name,
                                mount.mountPoint,
                                mount.usedPercent.toFloat()
                            )
                        }
                } catch (e: Exception) {
                    NotificationHelper.sendUnreachableAlert(applicationContext, server.name)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
