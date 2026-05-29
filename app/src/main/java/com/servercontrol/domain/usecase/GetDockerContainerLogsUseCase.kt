package com.servercontrol.domain.usecase

import com.servercontrol.domain.repository.DockerRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetDockerContainerLogsUseCase @Inject constructor(
    private val repository: DockerRepository
) {
    suspend operator fun invoke(serverId: Long, containerId: String, lines: Int = 100): Resource<List<String>> =
        repository.getContainerLogs(serverId, containerId, lines)
}
