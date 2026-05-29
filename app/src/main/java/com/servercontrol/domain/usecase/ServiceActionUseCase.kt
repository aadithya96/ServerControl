package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.ServiceAction
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ServiceActionUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    operator fun invoke(serverId: Long, name: String, action: ServiceAction): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        emit(repository.serviceAction(serverId, name, action))
    }
}
