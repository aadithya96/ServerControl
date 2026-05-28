package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.SystemStats

data class SystemStatsDto(
    @SerializedName("hostname") val hostname: String = "",
    @SerializedName("uptime_seconds") val uptimeSeconds: Long = 0,
    @SerializedName("load_avg_1") val loadAvg1: Double = 0.0,
    @SerializedName("load_avg_5") val loadAvg5: Double = 0.0,
    @SerializedName("load_avg_15") val loadAvg15: Double = 0.0,
    @SerializedName("cpu_percent") val cpuPercent: Double = 0.0,
    @SerializedName("cpu_cores") val cpuCores: Int = 0,
    @SerializedName("mem_total_bytes") val memTotalBytes: Long = 0,
    @SerializedName("mem_used_bytes") val memUsedBytes: Long = 0,
    @SerializedName("mem_free_bytes") val memFreeBytes: Long = 0,
    @SerializedName("swap_total_bytes") val swapTotalBytes: Long = 0,
    @SerializedName("swap_used_bytes") val swapUsedBytes: Long = 0
)

fun SystemStatsDto.toDomain(): SystemStats = SystemStats(
    hostname = hostname,
    uptimeSeconds = uptimeSeconds,
    loadAvg1m = loadAvg1,
    loadAvg5m = loadAvg5,
    loadAvg15m = loadAvg15,
    cpuPercent = cpuPercent,
    cpuCores = cpuCores,
    memTotalBytes = memTotalBytes,
    memUsedBytes = memUsedBytes,
    memFreeBytes = memFreeBytes,
    swapTotalBytes = swapTotalBytes,
    swapUsedBytes = swapUsedBytes
)
