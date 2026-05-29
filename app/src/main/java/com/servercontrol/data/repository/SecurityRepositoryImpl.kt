package com.servercontrol.data.repository

import com.servercontrol.data.local.db.AuditLogDao
import com.servercontrol.data.local.db.ServerProfileDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.dto.toDomain
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuditLogEntry
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.FailedLoginAttempt
import com.servercontrol.domain.model.SslCertInfo
import com.servercontrol.domain.repository.SecurityRepository
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SecurityRepositoryImpl @Inject constructor(
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val serverProfileDao: ServerProfileDao,
    private val auditLogDao: AuditLogDao
) : SecurityRepository {

    private suspend fun getServer(serverId: Long) =
        serverProfileDao.getServerById(serverId)?.toDomain()

    override suspend fun getFailedLogins(serverId: Long, limit: Int): Resource<List<FailedLoginAttempt>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getFailedLogins(server, limit)) {
                    is Resource.Success -> Resource.Success(result.data.attempts.map { it.toDomain() })
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.getFailedLogins(server, limit).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(result.message, result.throwable) }
                            )
                        } else Resource.Error(result.message, result.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getFailedLogins(server, limit).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Failed logins fetch failed", it) }
            )
        }
    }

    override suspend fun getSslCerts(serverId: Long, domains: String): Resource<List<SslCertInfo>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getSslCerts(server, domains)) {
                    is Resource.Success -> Resource.Success(result.data.certificates.map { it.toDomain() })
                    is Resource.Error -> Resource.Error(result.message, result.throwable)
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> Resource.Error("SSL cert check requires agent mode")
        }
    }

    override suspend fun blockIp(serverId: Long, ip: String): Resource<String> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.blockIp(server, ip)) {
                    is Resource.Success -> {
                        if (result.data.success) Resource.Success("IP $ip blocked successfully")
                        else Resource.Error(result.data.message)
                    }
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.blockIp(server, ip).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(result.message, result.throwable) }
                            )
                        } else Resource.Error(result.message, result.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.blockIp(server, ip).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Block IP failed", it) }
            )
        }
    }

    override fun getAuditLog(serverId: Long?): Flow<List<AuditLogEntry>> {
        return if (serverId != null) {
            auditLogDao.getForServer(serverId.toString()).map { list -> list.map { it.toDomain() } }
        } else {
            auditLogDao.getAll().map { list -> list.map { it.toDomain() } }
        }
    }
}
