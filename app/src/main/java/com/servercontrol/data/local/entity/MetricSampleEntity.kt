package com.servercontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.servercontrol.domain.model.MetricSample

@Entity(tableName = "metric_samples")
data class MetricSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String,
    val timestamp: Long,
    val cpuPercent: Float,
    val memUsedBytes: Long,
    val memTotalBytes: Long,
    val diskUsedBytes: Long,
    val diskTotalBytes: Long
)

fun MetricSampleEntity.toDomain() = MetricSample(
    serverId = serverId,
    timestamp = timestamp,
    cpuPercent = cpuPercent,
    memUsedBytes = memUsedBytes,
    memTotalBytes = memTotalBytes,
    diskUsedBytes = diskUsedBytes,
    diskTotalBytes = diskTotalBytes
)

fun MetricSample.toEntity() = MetricSampleEntity(
    serverId = serverId,
    timestamp = timestamp,
    cpuPercent = cpuPercent,
    memUsedBytes = memUsedBytes,
    memTotalBytes = memTotalBytes,
    diskUsedBytes = diskUsedBytes,
    diskTotalBytes = diskTotalBytes
)
