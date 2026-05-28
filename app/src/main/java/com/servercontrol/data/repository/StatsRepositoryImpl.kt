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
                when (val result = agentDataSource.getStats(server)) {
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
                when (val result = agentDataSource.getProcesses(server, sort = "cpu")) {
                    is Resource.Success -> Resource.Success(result.data.processes.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getProcesses(server)
        }
    }

    /**
     * Overload that accepts a sort parameter for the agent endpoint.
     */
    suspend fun getProcesses(serverId: Long, sort: String): Resource<List<Process>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getProcesses(server, sort = sort)) {
                    is Resource.Success -> Resource.Success(result.data.processes.map { it.toDomain() })
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
                when (val result = agentDataSource.getDiskInfo(server)) {
                    is Resource.Success -> Resource.Success(result.data.mounts.map { it.toDomain() })
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
                when (val result = agentDataSource.getConnections(server)) {
                    is Resource.Success -> Resource.Success(result.data.connections.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getConnections(server)
        }
    }

    /**
     * Overload that accepts a proto filter for the agent endpoint.
     */
    suspend fun getConnections(serverId: Long, proto: String): Resource<List<NetworkConnection>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getConnections(server, proto)) {
                    is Resource.Success -> Resource.Success(result.data.connections.map { it.toDomain() })
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
                when (val result = agentDataSource.getFirewallRules(server)) {
                    is Resource.Success -> Resource.Success(result.data.toDomainList())
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
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.killProcess(server, pid)) {
                    is Resource.Success -> {
                        if (result.data.success) Resource.Success(Unit)
                        else Resource.Error(result.data.message)
                    }
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.killProcess(server, pid)
        }
    }

    override suspend fun toggleFirewallRule(
        serverId: Long,
        ruleId: String,
        enable: Boolean
    ): Resource<Unit> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.toggleFirewallRule(server, ruleId, enable)) {
                    is Resource.Success -> {
                        if (result.data.success) Resource.Success(Unit)
                        else Resource.Error(result.data.message)
                    }
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> Resource.Error("Firewall toggle requires agent — SSH toggle not supported")
        }
    }
}
