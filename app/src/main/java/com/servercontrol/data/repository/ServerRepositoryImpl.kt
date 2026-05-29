package com.servercontrol.data.repository

import com.servercontrol.data.local.db.ServerProfileDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.local.entity.toEntity
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ServerRepositoryImpl @Inject constructor(
    private val serverProfileDao: ServerProfileDao,
    private val agentDataSource: AgentDataSource
) : ServerRepository {

    override fun getServers(): Flow<List<ServerProfile>> =
        serverProfileDao.getAllServers().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getServerById(id: Long): ServerProfile? =
        serverProfileDao.getServerById(id)?.toDomain()

    override suspend fun insertServer(server: ServerProfile): Long =
        serverProfileDao.insertServer(server.toEntity())

    override suspend fun updateServer(server: ServerProfile) =
        serverProfileDao.updateServer(server.toEntity())

    override suspend fun deleteServer(server: ServerProfile) =
        serverProfileDao.deleteServer(server.toEntity())

    override suspend fun getAllServers(): List<ServerProfile> =
        serverProfileDao.getAllServersOnce().map { it.toDomain() }

    override suspend fun setServerGroup(serverId: Long, group: String) =
        serverProfileDao.setServerGroup(serverId, group)

    override fun getDistinctGroups(): Flow<List<String>> =
        serverProfileDao.getDistinctGroups()

    override suspend fun testConnection(server: ServerProfile): Result<Long> {
        val start = System.currentTimeMillis()
        return try {
            when (server.authType) {
                AuthType.AGENT_TOKEN -> {
                    agentDataSource.getSystemStats(server)
                    Result.success(System.currentTimeMillis() - start)
                }
                else -> {
                    // Attempt SSH by connecting and immediately disconnecting
                    com.jcraft.jsch.JSch().run {
                        val session = when (server.authType) {
                            AuthType.SSH_KEY -> {
                                server.sshPrivateKey?.let { key ->
                                    addIdentity("key", key.toByteArray(), null, null)
                                }
                                getSession(server.sshUsername ?: "root", server.host, server.sshPort)
                            }
                            else -> {
                                getSession(server.sshUsername ?: "root", server.host, server.sshPort).also {
                                    it.setPassword(server.sshPassword ?: "")
                                }
                            }
                        }
                        session.setConfig("StrictHostKeyChecking", "no")
                        session.connect(10_000)
                        session.disconnect()
                    }
                    Result.success(System.currentTimeMillis() - start)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
