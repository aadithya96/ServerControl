package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.ProcessSortOrder
import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetProcessesUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(
        serverId: Long,
        sortOrder: ProcessSortOrder = ProcessSortOrder.CPU
    ): Resource<List<Process>> {
        val result = repository.getProcesses(serverId)
        return if (result is Resource.Success) {
            val sorted = when (sortOrder) {
                ProcessSortOrder.CPU -> result.data.sortedByDescending { it.cpuPercent }
                ProcessSortOrder.MEMORY -> result.data.sortedByDescending { it.memPercent }
                ProcessSortOrder.PID -> result.data.sortedBy { it.pid }
                ProcessSortOrder.NAME -> result.data.sortedBy { it.name }
            }
            Resource.Success(sorted)
        } else result
    }
}
