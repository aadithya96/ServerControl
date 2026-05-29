package com.servercontrol.util

import com.servercontrol.data.local.db.AuditLogDao
import com.servercontrol.data.local.entity.AuditLogEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditLogger @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    suspend fun log(
        serverId: String,
        serverName: String,
        action: String,
        details: String,
        result: String = "success"
    ) {
        auditLogDao.insert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                serverId = serverId,
                serverName = serverName,
                action = action,
                details = details,
                result = result
            )
        )
    }
}
