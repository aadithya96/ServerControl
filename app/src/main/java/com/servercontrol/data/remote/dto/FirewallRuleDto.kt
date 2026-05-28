package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.FirewallRule

data class FirewallRuleItemDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("num") val num: Int = 0,
    @SerializedName("target") val target: String = "",
    @SerializedName("protocol") val protocol: String = "",
    @SerializedName("source") val source: String = "",
    @SerializedName("destination") val destination: String = "",
    @SerializedName("options") val options: String = "",
    @SerializedName("packets") val packets: Long = 0,
    @SerializedName("bytes") val bytes: Long = 0
)

data class FirewallChainDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("policy") val policy: String = "",
    @SerializedName("rules") val rules: List<FirewallRuleItemDto> = emptyList()
)

data class FirewallResponseDto(
    @SerializedName("backend") val backend: String = "",
    @SerializedName("chains") val chains: List<FirewallChainDto> = emptyList()
)

fun FirewallRuleItemDto.toDomain(chain: String): FirewallRule = FirewallRule(
    id = id,
    chain = chain,
    target = target,
    protocol = protocol,
    source = source,
    destination = destination,
    options = options,
    enabled = true,
    packetsCount = packets,
    bytesCount = bytes
)

fun FirewallResponseDto.toDomainList(): List<FirewallRule> =
    chains.flatMap { chain -> chain.rules.map { it.toDomain(chain.name) } }
