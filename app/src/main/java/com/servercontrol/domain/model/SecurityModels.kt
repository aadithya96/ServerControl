package com.servercontrol.domain.model

import java.util.UUID

data class FailedLoginAttempt(
    val timestamp: String,
    val username: String,
    val sourceIp: String,
    val count: Int
)

data class IpInfo(
    val ip: String,
    val country: String,
    val city: String,
    val isp: String
)

data class SslCertInfo(
    val domain: String,
    val expiryDate: Long,
    val daysUntilExpiry: Int,
    val issuer: String,
    val isValid: Boolean
)

data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val serverId: String,
    val serverName: String,
    val action: String,
    val details: String,
    val result: String
)
