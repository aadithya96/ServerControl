package com.servercontrol.domain.model

data class NetworkConnection(
    val protocol: String,
    val localAddress: String,
    val localPort: Int,
    val remoteAddress: String,
    val remotePort: Int,
    val state: String,
    val pid: Int?,
    val processName: String?
)
