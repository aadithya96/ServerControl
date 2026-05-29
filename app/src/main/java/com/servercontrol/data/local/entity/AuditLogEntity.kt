package com.servercontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.servercontrol.domain.model.AuditLogEntry

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val serverId: String,
    val serverName: String,
    val action: String,
    val details: String,
    val result: String
)

fun AuditLogEntity.toDomain() = AuditLogEntry(
    id = id,
    timestamp = timestamp,
    serverId = serverId,
    serverName = serverName,
    action = action,
    details = details,
    result = result
)
