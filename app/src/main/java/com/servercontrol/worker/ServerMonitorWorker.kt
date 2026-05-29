package com.servercontrol.worker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.servercontrol.data.remote.WebhookService
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
import androidx.datastore.preferences.core.stringPreferencesKey as strKey

@HiltWorker
class ServerMonitorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val serverRepository: ServerRepository,
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val dataStore: DataStore<Preferences>,
    private val webhookService: WebhookService
) : CoroutineWorker(context, params) {

    private object Keys {
        val CPU_ALERT = intPreferencesKey("cpu_alert_threshold")
        val DISK_ALERT = intPreferencesKey("disk_alert_threshold")
        val WEBHOOK_TYPE = strKey("webhook_type")
        val WEBHOOK_URL = strKey("webhook_url")
        val TELEGRAM_BOT_TOKEN = strKey("telegram_bot_token")
        val TELEGRAM_CHAT_ID = strKey("telegram_chat_id")
    }

    override suspend fun doWork(): Result {
        return try {
            val prefs = dataStore.data.first()
            val cpuThreshold = prefs[Keys.CPU_ALERT] ?: 80
            val diskThreshold = prefs[Keys.DISK_ALERT] ?: 90
            val webhookType = prefs[Keys.WEBHOOK_TYPE] ?: "none"
            val webhookUrl = prefs[Keys.WEBHOOK_URL] ?: ""
            val telegramToken = prefs[Keys.TELEGRAM_BOT_TOKEN] ?: ""
            val telegramChatId = prefs[Keys.TELEGRAM_CHAT_ID] ?: ""

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
                        val msg = "CPU at ${"%.0f".format(cpuPercent)}% (threshold: $cpuThreshold%)"
                        sendWebhookAlert(webhookType, webhookUrl, telegramToken, telegramChatId, msg, server.name)
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
                            val msg = "Disk ${mount.mountPoint} at ${"%.0f".format(mount.usedPercent)}% (threshold: $diskThreshold%)"
                            sendWebhookAlert(webhookType, webhookUrl, telegramToken, telegramChatId, msg, server.name)
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

    private suspend fun sendWebhookAlert(
        type: String,
        url: String,
        telegramToken: String,
        telegramChatId: String,
        message: String,
        serverName: String
    ) {
        try {
            when (type) {
                "slack" -> if (url.isNotBlank()) webhookService.sendSlackAlert(url, message, serverName)
                "discord" -> if (url.isNotBlank()) webhookService.sendDiscordAlert(url, message, serverName)
                "telegram" -> if (telegramToken.isNotBlank() && telegramChatId.isNotBlank()) {
                    webhookService.sendTelegramAlert(telegramToken, telegramChatId, message, serverName)
                }
            }
        } catch (_: Exception) {}
    }
}
