package com.servercontrol.domain.usecase

import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class KillProcessUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(serverId: Long, pid: Int): Resource<Unit> =
        repository.killProcess(serverId, pid)
}
