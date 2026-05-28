package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServersUseCase @Inject constructor(
    private val repository: ServerRepository
) {
    operator fun invoke(): Flow<List<ServerProfile>> = repository.getServers()
}
