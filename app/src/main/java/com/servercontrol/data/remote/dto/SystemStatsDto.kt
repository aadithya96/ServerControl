package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.SystemStats

data class SystemStatsDto(
    @SerializedName("cpu_percent") val cpuPercent: Double,
    @SerializedName("mem_used") val memUsedBytes: Long,
    @SerializedName("mem_total") val memTotalBytes: Long,
    @SerializedName("swap_used") val swapUsedBytes: Long,
    @SerializedName("swap_total") val swapTotalBytes: Long,
    @SerializedName("uptime_seconds") val uptimeSeconds: Long,
    @SerializedName("load_avg_1m") val loadAvg1m: Double,
    @SerializedName("load_avg_5m") val loadAvg5m: Double,
    @SerializedName("load_avg_15m") val loadAvg15m: Double
)

fun SystemStatsDto.toDomain(): SystemStats = SystemStats(
    cpuPercent = cpuPercent,
    memUsedBytes = memUsedBytes,
    memTotalBytes = memTotalBytes,
    swapUsedBytes = swapUsedBytes,
    swapTotalBytes = swapTotalBytes,
    uptimeSeconds = uptimeSeconds,
    loadAvg1m = loadAvg1m,
    loadAvg5m = loadAvg5m,
    loadAvg15m = loadAvg15m
)
