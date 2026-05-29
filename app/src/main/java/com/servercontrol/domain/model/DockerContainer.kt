package com.servercontrol.domain.model

data class DockerContainer(
    val id: String,
    val shortId: String,
    val name: String,
    val image: String,
    val status: String,
    val state: String,
    val created: Long,
    val ports: List<PortMapping>,
    val cpuPercent: Double,
    val memUsedBytes: Long,
    val memLimitBytes: Long,
    val networkRxBytes: Long,
    val networkTxBytes: Long,
    val mounts: List<String>,
    val envVars: List<String>
)

data class PortMapping(
    val containerPort: Int,
    val hostPort: Int,
    val protocol: String
)

data class DockerImage(
    val id: String,
    val tags: List<String>,
    val sizeBytes: Long,
    val created: Long
)

enum class DockerAction { START, STOP, RESTART, REMOVE, PAUSE, UNPAUSE }
