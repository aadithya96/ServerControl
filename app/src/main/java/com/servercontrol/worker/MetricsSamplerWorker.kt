package com.servercontrol.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.MetricSample
import com.servercontrol.domain.repository.MetricsRepository
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MetricsSamplerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val metricsRepository: MetricsRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val servers = serverRepository.getAllServers()
            val now = System.currentTimeMillis()

            for (server in servers) {
                try {
                    // Fetch system stats for CPU and memory
                    val stats = if (server.authType == AuthType.AGENT_TOKEN) {
                        when (val r = agentDataSource.getStats(server)) {
                            is Resource.Success -> r.data
                            else -> null
                        }
                    } else {
                        null
                    }

                    val cpuPercent = stats?.cpuPercent?.toFloat()
                        ?: sshDataSource.getSystemStats(server).getOrNull()?.cpuPercent?.toFloat()
                        ?: continue

                    val memUsed = stats?.memUsedBytes?.toLong()
                        ?: sshDataSource.getSystemStats(server).getOrNull()?.memUsedBytes
                        ?: 0L
                    val memTotal = stats?.memTotalBytes?.toLong()
                        ?: sshDataSource.getSystemStats(server).getOrNull()?.memTotalBytes
                        ?: 0L

                    // Fetch disk info for disk usage (sum of used/total across all mounts)
                    val diskPair: Pair<Long, Long> = if (server.authType == AuthType.AGENT_TOKEN) {
                        when (val r = agentDataSource.getDiskInfo(server)) {
                            is Resource.Success -> {
                                val mounts = r.data.mounts
                                Pair(
                                    mounts.sumOf { it.usedBytes },
                                    mounts.sumOf { it.totalBytes }
                                )
                            }
                            else -> Pair(0L, 0L)
                        }
                    } else {
                        sshDataSource.getDiskInfo(server).getOrNull()?.let { disks ->
                            Pair(
                                disks.sumOf { it.usedBytes },
                                disks.sumOf { it.totalBytes }
                            )
                        } ?: Pair(0L, 0L)
                    }

                    metricsRepository.saveSample(
                        MetricSample(
                            serverId = server.id.toString(),
                            timestamp = now,
                            cpuPercent = cpuPercent,
                            memUsedBytes = memUsed,
                            memTotalBytes = memTotal,
                            diskUsedBytes = diskPair.first,
                            diskTotalBytes = diskPair.second
                        )
                    )
                } catch (e: Exception) {
                    // Continue with other servers if one fails
                }
            }

            // Prune old samples (older than 7 days)
            metricsRepository.pruneOldSamples(keepDays = 7)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
