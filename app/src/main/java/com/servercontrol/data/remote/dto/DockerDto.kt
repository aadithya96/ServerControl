package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.model.DockerImage
import com.servercontrol.domain.model.PortMapping

data class DockerContainerDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("short_id") val shortId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("state") val state: String = "",
    @SerializedName("created") val created: Long = 0,
    @SerializedName("ports") val ports: List<PortMappingDto> = emptyList(),
    @SerializedName("cpu_percent") val cpuPercent: Double = 0.0,
    @SerializedName("mem_used_bytes") val memUsedBytes: Long = 0,
    @SerializedName("mem_limit_bytes") val memLimitBytes: Long = 0,
    @SerializedName("network_rx_bytes") val networkRxBytes: Long = 0,
    @SerializedName("network_tx_bytes") val networkTxBytes: Long = 0
)

data class PortMappingDto(
    @SerializedName("container_port") val containerPort: Int = 0,
    @SerializedName("host_port") val hostPort: Int = 0,
    @SerializedName("protocol") val protocol: String = "tcp"
)

data class DockerContainersResponseDto(
    @SerializedName("containers") val containers: List<DockerContainerDto> = emptyList()
)

data class DockerImageDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("size_bytes") val sizeBytes: Long = 0,
    @SerializedName("created") val created: Long = 0
)

data class DockerImagesResponseDto(
    @SerializedName("images") val images: List<DockerImageDto> = emptyList()
)

data class DockerActionRequest(
    @SerializedName("action") val action: String
)

data class DockerLogsResponseDto(
    @SerializedName("lines") val lines: List<String> = emptyList()
)

data class GenericResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)

fun DockerContainerDto.toDomain() = DockerContainer(
    id = id,
    shortId = shortId,
    name = name,
    image = image,
    status = status,
    state = state,
    created = created,
    ports = ports.map { PortMapping(it.containerPort, it.hostPort, it.protocol) },
    cpuPercent = cpuPercent,
    memUsedBytes = memUsedBytes,
    memLimitBytes = memLimitBytes,
    networkRxBytes = networkRxBytes,
    networkTxBytes = networkTxBytes,
    mounts = emptyList(),
    envVars = emptyList()
)

fun DockerImageDto.toDomain() = DockerImage(
    id = id,
    tags = tags,
    sizeBytes = sizeBytes,
    created = created
)
