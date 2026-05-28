package com.servercontrol.domain.model

data class SystemStats(
    val hostname: String = "",
    val uptimeSeconds: Long = 0,
    val loadAvg1m: Double = 0.0,
    val loadAvg5m: Double = 0.0,
    val loadAvg15m: Double = 0.0,
    val cpuPercent: Double = 0.0,
    val cpuCores: Int = 0,
    val memTotalBytes: Long = 0,
    val memUsedBytes: Long = 0,
    val memFreeBytes: Long = 0,
    val swapTotalBytes: Long = 0,
    val swapUsedBytes: Long = 0
) {
    val memPercent: Double
        get() = if (memTotalBytes > 0) memUsedBytes.toDouble() / memTotalBytes * 100 else 0.0

    val swapPercent: Double
        get() = if (swapTotalBytes > 0) swapUsedBytes.toDouble() / swapTotalBytes * 100 else 0.0
}
