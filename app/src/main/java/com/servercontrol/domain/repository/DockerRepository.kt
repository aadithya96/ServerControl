package com.servercontrol.domain.repository

import com.servercontrol.domain.model.DockerAction
import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.model.DockerImage
import com.servercontrol.util.Resource

interface DockerRepository {
    suspend fun getContainers(serverId: Long): Resource<List<DockerContainer>>
    suspend fun getImages(serverId: Long): Resource<List<DockerImage>>
    suspend fun containerAction(serverId: Long, containerId: String, action: DockerAction): Resource<String>
    suspend fun getContainerLogs(serverId: Long, containerId: String, lines: Int = 100): Resource<List<String>>
}
