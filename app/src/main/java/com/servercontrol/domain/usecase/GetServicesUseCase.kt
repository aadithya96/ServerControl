package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.SystemService
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetServicesUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    operator fun invoke(serverId: Long, type: String? = null, state: String? = null): Flow<Resource<List<SystemService>>> = flow {
        emit(Resource.Loading)
        emit(repository.getServices(serverId, type, state))
    }
}
