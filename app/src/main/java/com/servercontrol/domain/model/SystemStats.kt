package com.servercontrol.domain.model

data class SystemStats(
    val cpuPercent: Double,
    val memUsedBytes: Long,
    val memTotalBytes: Long,
    val swapUsedBytes: Long,
    val swapTotalBytes: Long,
    val uptimeSeconds: Long,
    val loadAvg1m: Double,
    val loadAvg5m: Double,
    val loadAvg15m: Double
) {
    val memPercent: Double
        get() = if (memTotalBytes > 0) memUsedBytes.toDouble() / memTotalBytes * 100 else 0.0

    val swapPercent: Double
        get() = if (swapTotalBytes > 0) swapUsedBytes.toDouble() / swapTotalBytes * 100 else 0.0
}
