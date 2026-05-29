package com.servercontrol.domain.usecase

import com.servercontrol.domain.repository.MetricsRepository
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ExportMetricsCsvUseCase @Inject constructor(
    private val repository: MetricsRepository
) {
    operator fun invoke(serverId: String, rangeHours: Int): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        try {
            val since = System.currentTimeMillis() - rangeHours * 60 * 60 * 1000L
            val csv = repository.exportCsv(serverId, since)
            emit(Resource.Success(csv))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to export CSV", e))
        }
    }
}
