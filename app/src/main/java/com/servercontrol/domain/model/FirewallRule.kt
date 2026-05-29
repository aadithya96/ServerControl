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

data class FirewallData(
    val backend: String,
    val chains: List<FirewallChain>
)

data class FirewallChain(
    val name: String,
    val policy: String,
    val rules: List<FirewallRule>
)
