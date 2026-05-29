package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.AuditLogEntry
import com.servercontrol.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAuditLogUseCase @Inject constructor(
    private val repository: SecurityRepository
) {
    operator fun invoke(serverId: Long? = null): Flow<List<AuditLogEntry>> =
        repository.getAuditLog(serverId)
}
