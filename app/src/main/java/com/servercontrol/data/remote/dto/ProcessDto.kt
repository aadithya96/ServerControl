package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.Process

data class ProcessDto(
    @SerializedName("pid") val pid: Int,
    @SerializedName("name") val name: String,
    @SerializedName("user") val user: String,
    @SerializedName("cpu_percent") val cpuPercent: Double,
    @SerializedName("mem_percent") val memPercent: Double,
    @SerializedName("mem_rss") val memRss: Long,
    @SerializedName("command") val command: String,
    @SerializedName("status") val status: String
)

fun ProcessDto.toDomain(): Process = Process(
    pid = pid,
    name = name,
    user = user,
    cpuPercent = cpuPercent,
    memPercent = memPercent,
    memRss = memRss,
    command = command,
    status = status
)
