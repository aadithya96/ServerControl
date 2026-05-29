package com.servercontrol.domain.repository

import com.servercontrol.domain.model.AuditLogEntry
import com.servercontrol.domain.model.FailedLoginAttempt
import com.servercontrol.domain.model.SslCertInfo
import com.servercontrol.util.Resource
import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    suspend fun getFailedLogins(serverId: Long, limit: Int = 50): Resource<List<FailedLoginAttempt>>
    suspend fun getSslCerts(serverId: Long, domains: String): Resource<List<SslCertInfo>>
    suspend fun blockIp(serverId: Long, ip: String): Resource<String>
    fun getAuditLog(serverId: Long?): Flow<List<AuditLogEntry>>
}
