package com.servercontrol.domain.usecase

import com.servercontrol.domain.repository.SecurityRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class BlockIpUseCase @Inject constructor(
    private val repository: SecurityRepository
) {
    suspend operator fun invoke(serverId: Long, ip: String): Resource<String> =
        repository.blockIp(serverId, ip)
}
