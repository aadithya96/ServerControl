package com.servercontrol.domain.model

data class Process(
    val pid: Int,
    val name: String,
    val user: String,
    val cpuPercent: Double,
    val memPercent: Double,
    val memRss: Long,
    val command: String,
    val status: String
)

enum class ProcessSortOrder { CPU, MEMORY, PID, NAME }
