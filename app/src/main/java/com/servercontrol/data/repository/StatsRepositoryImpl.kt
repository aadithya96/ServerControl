package com.servercontrol.data.repository

import com.servercontrol.data.local.db.ServerProfileDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.dto.*
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.*
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val serverProfileDao: ServerProfileDao
) : StatsRepository {

    private suspend fun getServer(serverId: Long): ServerProfile? =
        serverProfileDao.getServerById(serverId)?.toDomain()

    override suspend fun getSystemStats(serverId: Long): Resource<SystemStats> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                val result = agentDataSource.getSystemStats(server)
                when (result) {
                    is Resource.Success -> Resource.Success(result.data.toDomain())
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getSystemStats(server)
        }
    }

    override suspend fun getProcesses(serverId: Long): Resource<List<Process>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                val result = agentDataSource.getProcesses(server)
                when (result) {
                    is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getProcesses(server)
        }
    }

    override suspend fun getDiskInfo(serverId: Long): Resource<List<DiskInfo>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                val result = agentDataSource.getDiskInfo(server)
                when (result) {
                    is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getDiskInfo(server)
        }
    }

    override suspend fun getConnections(serverId: Long): Resource<List<NetworkConnection>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                val result = agentDataSource.getConnections(server)
                when (result) {
                    is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getConnections(server)
        }
    }

    override suspend fun getFirewallRules(serverId: Long): Resource<List<FirewallRule>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                val result = agentDataSource.getFirewallRules(server)
                when (result) {
                    is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getFirewallRules(server)
        }
    }

    override suspend fun killProcess(serverId: Long, pid: Int): Resource<Unit> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> agentDataSource.killProcess(server, pid)
            else -> sshDataSource.killProcess(server, pid)
        }
    }

    override suspend fun toggleFirewallRule(
        serverId: Long, ruleId: String, enable: Boolean
    ): Resource<Unit> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> agentDataSource.toggleFirewallRule(server, ruleId, enable)
            else -> Resource.Error("Firewall toggle requires agent — SSH toggle not supported")
        }
    }
}
