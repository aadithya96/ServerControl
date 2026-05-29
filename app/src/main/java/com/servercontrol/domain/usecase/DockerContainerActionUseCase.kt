package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.DockerAction
import com.servercontrol.domain.repository.DockerRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class DockerContainerActionUseCase @Inject constructor(
    private val repository: DockerRepository
) {
    suspend operator fun invoke(serverId: Long, containerId: String, action: DockerAction): Resource<String> =
        repository.containerAction(serverId, containerId, action)
}
