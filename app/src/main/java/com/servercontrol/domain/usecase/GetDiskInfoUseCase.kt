package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetDiskInfoUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(serverId: Long): Resource<List<DiskInfo>> =
        repository.getDiskInfo(serverId)
}
