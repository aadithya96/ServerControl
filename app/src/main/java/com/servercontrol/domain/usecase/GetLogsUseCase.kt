package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.LogEntry
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetLogsUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    operator fun invoke(serverId: Long, source: String, unit: String?, lines: Int = 200): Flow<Resource<List<LogEntry>>> = flow {
        emit(Resource.Loading)
        emit(repository.getLogs(serverId, source, unit, lines))
    }
}
