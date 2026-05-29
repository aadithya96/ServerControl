package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.repository.DockerRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetDockerContainersUseCase @Inject constructor(
    private val repository: DockerRepository
) {
    suspend operator fun invoke(serverId: Long): Resource<List<DockerContainer>> =
        repository.getContainers(serverId)
}
