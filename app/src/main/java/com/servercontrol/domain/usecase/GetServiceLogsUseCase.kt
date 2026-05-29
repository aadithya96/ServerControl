package com.servercontrol.domain.usecase

import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetServiceLogsUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    operator fun invoke(serverId: Long, name: String, lines: Int = 100): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading)
        emit(repository.getServiceLogs(serverId, name, lines))
    }
}
