package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetConnectionsUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(serverId: Long): Resource<List<NetworkConnection>> =
        repository.getConnections(serverId)
}
