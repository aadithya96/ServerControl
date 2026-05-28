package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.NetworkConnection

data class ConnectionDto(
    @SerializedName("protocol") val protocol: String,
    @SerializedName("local_address") val localAddress: String,
    @SerializedName("local_port") val localPort: Int,
    @SerializedName("remote_address") val remoteAddress: String,
    @SerializedName("remote_port") val remotePort: Int,
    @SerializedName("state") val state: String,
    @SerializedName("pid") val pid: Int?,
    @SerializedName("process_name") val processName: String?
)

fun ConnectionDto.toDomain(): NetworkConnection = NetworkConnection(
    protocol = protocol,
    localAddress = localAddress,
    localPort = localPort,
    remoteAddress = remoteAddress,
    remotePort = remotePort,
    state = state,
    pid = pid,
    processName = processName
)
