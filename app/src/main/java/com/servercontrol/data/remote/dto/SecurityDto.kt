package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.FailedLoginAttempt
import com.servercontrol.domain.model.SslCertInfo

data class FailedLoginDto(
    @SerializedName("source_ip") val sourceIp: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("count") val count: Int = 0,
    @SerializedName("last_seen") val lastSeen: String = ""
)

data class FailedLoginsResponseDto(
    @SerializedName("attempts") val attempts: List<FailedLoginDto> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

data class SslCertDto(
    @SerializedName("domain") val domain: String = "",
    @SerializedName("expiry_unix") val expiryUnix: Long = 0,
    @SerializedName("days_until_expiry") val daysUntilExpiry: Int = 0,
    @SerializedName("issuer") val issuer: String = "",
    @SerializedName("is_valid") val isValid: Boolean = false
)

data class SslCertsResponseDto(
    @SerializedName("certificates") val certificates: List<SslCertDto> = emptyList()
)

data class BlockIpRequest(
    @SerializedName("ip") val ip: String
)

fun FailedLoginDto.toDomain() = FailedLoginAttempt(
    timestamp = lastSeen,
    username = username,
    sourceIp = sourceIp,
    count = count
)

fun SslCertDto.toDomain() = SslCertInfo(
    domain = domain,
    expiryDate = expiryUnix * 1000L,
    daysUntilExpiry = daysUntilExpiry,
    issuer = issuer,
    isValid = isValid
)
