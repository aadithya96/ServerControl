package com.servercontrol.domain.model

data class MetricSample(
    val serverId: String,
    val timestamp: Long,
    val cpuPercent: Float,
    val memUsedBytes: Long,
    val memTotalBytes: Long,
    val diskUsedBytes: Long,
    val diskTotalBytes: Long
)
