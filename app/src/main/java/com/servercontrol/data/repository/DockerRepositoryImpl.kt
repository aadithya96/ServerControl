package com.servercontrol.data.repository

import com.servercontrol.data.local.db.ServerProfileDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.dto.toDomain
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.DockerAction
import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.model.DockerImage
import com.servercontrol.domain.repository.DockerRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class DockerRepositoryImpl @Inject constructor(
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val serverProfileDao: ServerProfileDao
) : DockerRepository {

    private suspend fun getServer(serverId: Long) =
        serverProfileDao.getServerById(serverId)?.toDomain()

    override suspend fun getContainers(serverId: Long): Resource<List<DockerContainer>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getDockerContainers(server)) {
                    is Resource.Success -> Resource.Success(result.data.containers.map { it.toDomain() })
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.getDockerContainers(server).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(result.message, result.throwable) }
                            )
                        } else Resource.Error(result.message, result.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getDockerContainers(server).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "SSH docker containers failed", it) }
            )
        }
    }

    override suspend fun getImages(serverId: Long): Resource<List<DockerImage>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getDockerImages(server)) {
                    is Resource.Success -> Resource.Success(result.data.images.map { it.toDomain() })
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.getDockerImages(server).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(result.message, result.throwable) }
                            )
                        } else Resource.Error(result.message, result.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getDockerImages(server).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "SSH docker images failed", it) }
            )
        }
    }

    override suspend fun containerAction(serverId: Long, containerId: String, action: DockerAction): Resource<String> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.dockerContainerAction(server, containerId, action)) {
                    is Resource.Success -> {
                        if (result.data.success) Resource.Success(result.data.message)
                        else Resource.Error(result.data.message)
                    }
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.dockerContainerAction(server, containerId, action).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(result.message, result.throwable) }
                            )
                        } else Resource.Error(result.message, result.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.dockerContainerAction(server, containerId, action).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Docker action failed", it) }
            )
        }
    }

    override suspend fun getContainerLogs(serverId: Long, containerId: String, lines: Int): Resource<List<String>> {
        val server = getServer(serverId) ?: return Resource.Error("Server not found")
        return when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val result = agentDataSource.getDockerContainerLogs(server, containerId, lines)) {
                    is Resource.Success -> Resource.Success(result.data.lines)
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.getDockerContainerLogs(server, containerId, lines).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(result.message, result.throwable) }
                            )
                        } else Resource.Error(result.message, result.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.getDockerContainerLogs(server, containerId, lines).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Docker logs failed", it) }
            )
        }
    }
}
