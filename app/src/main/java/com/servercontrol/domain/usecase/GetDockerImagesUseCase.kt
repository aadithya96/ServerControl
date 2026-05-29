package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.DockerImage
import com.servercontrol.domain.repository.DockerRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetDockerImagesUseCase @Inject constructor(
    private val repository: DockerRepository
) {
    suspend operator fun invoke(serverId: Long): Resource<List<DockerImage>> =
        repository.getImages(serverId)
}
