package com.servercontrol.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.servercontrol.data.local.entity.MetricSampleEntity

@Dao
interface MetricSampleDao {

    @Insert
    suspend fun insert(sample: MetricSampleEntity)

    @Query("SELECT * FROM metric_samples WHERE serverId = :serverId AND timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSince(serverId: String, since: Long): List<MetricSampleEntity>

    @Query("DELETE FROM metric_samples WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM metric_samples WHERE serverId = :serverId")
    suspend fun countForServer(serverId: String): Int
}
