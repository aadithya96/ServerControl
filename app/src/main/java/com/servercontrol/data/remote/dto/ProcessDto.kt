package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.Process

data class ProcessDto(
    @SerializedName("pid") val pid: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("user") val user: String = "",
    @SerializedName("cpu_percent") val cpuPercent: Double = 0.0,
    @SerializedName("mem_percent") val memPercent: Double = 0.0,
    @SerializedName("mem_rss_bytes") val memRssBytes: Long = 0,
    @SerializedName("status") val status: String = "",
    @SerializedName("command") val command: String = ""
)

data class ProcessResponseDto(
    @SerializedName("processes") val processes: List<ProcessDto> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

fun ProcessDto.toDomain(): Process = Process(
    pid = pid,
    name = name,
    user = user,
    cpuPercent = cpuPercent,
    memPercent = memPercent,
    memRss = memRssBytes,
    command = command,
    status = status
)
