package com.servercontrol.domain.model

data class FirewallRule(
    val id: String,
    val chain: String,
    val target: String,
    val protocol: String,
    val source: String,
    val destination: String,
    val options: String,
    val enabled: Boolean,
    val packetsCount: Long = 0,
    val bytesCount: Long = 0
)
