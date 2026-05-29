package com.servercontrol.data.local.db

import androidx.room.*
import com.servercontrol.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 500")
    fun getAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log WHERE serverId = :serverId ORDER BY timestamp DESC LIMIT 200")
    fun getForServer(serverId: String): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insert(entry: AuditLogEntity)

    @Query("DELETE FROM audit_log WHERE timestamp < :before")
    suspend fun pruneOlderThan(before: Long)
}
