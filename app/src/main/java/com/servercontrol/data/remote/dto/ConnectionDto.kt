package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.NetworkConnection

data class ConnectionItemDto(
    @SerializedName("protocol") val protocol: String = "",
    @SerializedName("local_address") val localAddress: String = "",
    @SerializedName("local_port") val localPort: Int = 0,
    @SerializedName("remote_address") val remoteAddress: String = "",
    @SerializedName("remote_port") val remotePort: Int = 0,
    @SerializedName("state") val state: String = "",
    @SerializedName("pid") val pid: Int? = null,
    @SerializedName("process_name") val processName: String? = null
)

data class ConnectionResponseDto(
    @SerializedName("connections") val connections: List<ConnectionItemDto> = emptyList()
)

fun ConnectionItemDto.toDomain(): NetworkConnection = NetworkConnection(
    protocol = protocol,
    localAddress = localAddress,
    localPort = localPort,
    remoteAddress = remoteAddress,
    remotePort = remotePort,
    state = state,
    pid = pid,
    processName = processName
)
