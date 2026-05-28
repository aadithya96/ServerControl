package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.FirewallRule

data class FirewallRuleDto(
    @SerializedName("id") val id: String,
    @SerializedName("chain") val chain: String,
    @SerializedName("target") val target: String,
    @SerializedName("protocol") val protocol: String,
    @SerializedName("source") val source: String,
    @SerializedName("destination") val destination: String,
    @SerializedName("options") val options: String,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("packets") val packetsCount: Long = 0,
    @SerializedName("bytes") val bytesCount: Long = 0
)

fun FirewallRuleDto.toDomain(): FirewallRule = FirewallRule(
    id = id,
    chain = chain,
    target = target,
    protocol = protocol,
    source = source,
    destination = destination,
    options = options,
    enabled = enabled,
    packetsCount = packetsCount,
    bytesCount = bytesCount
)
