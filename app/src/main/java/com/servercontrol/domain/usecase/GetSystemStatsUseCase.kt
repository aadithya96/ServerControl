package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.SystemStats
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetSystemStatsUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(serverId: Long): Resource<SystemStats> =
        repository.getSystemStats(serverId)
}
