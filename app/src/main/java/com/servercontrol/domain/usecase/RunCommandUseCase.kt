package com.servercontrol.domain.usecase

import com.servercontrol.data.local.db.ServerProfileDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RunCommandUseCase @Inject constructor(
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource,
    private val serverProfileDao: ServerProfileDao
) {
    operator fun invoke(serverId: Long, command: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        val server = serverProfileDao.getServerById(serverId)?.toDomain()
            ?: run { emit(Resource.Error("Server not found")); return@flow }
        val result = when (server.authType) {
            AuthType.AGENT_TOKEN -> {
                when (val r = agentDataSource.executeCommand(server, command)) {
                    is Resource.Success -> Resource.Success(r.data.output)
                    is Resource.Error -> {
                        if (server.sshUsername != null) {
                            sshDataSource.executeCommand(server, command).fold(
                                onSuccess = { Resource.Success(it) },
                                onFailure = { Resource.Error(r.message, r.throwable) }
                            )
                        } else Resource.Error(r.message, r.throwable)
                    }
                    is Resource.Loading -> Resource.Loading
                }
            }
            else -> sshDataSource.executeCommand(server, command).fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Command execution failed", it) }
            )
        }
        emit(result)
    }
}
