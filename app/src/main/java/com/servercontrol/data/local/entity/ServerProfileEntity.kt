package com.servercontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile

@Entity(tableName = "server_profiles")
data class ServerProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val agentPort: Int = 9876,
    val sshPort: Int = 22,
    val authType: String,
    val agentToken: String? = null,
    val sshUsername: String? = null,
    val sshPassword: String? = null,
    val sshPrivateKey: String? = null,
    val groupName: String = "default"
)

fun ServerProfileEntity.toDomain(): ServerProfile = ServerProfile(
    id = id,
    name = name,
    host = host,
    agentPort = agentPort,
    sshPort = sshPort,
    authType = AuthType.valueOf(authType),
    agentToken = agentToken,
    sshUsername = sshUsername,
    sshPassword = sshPassword,
    sshPrivateKey = sshPrivateKey,
    group = groupName
)

fun ServerProfile.toEntity(): ServerProfileEntity = ServerProfileEntity(
    id = id,
    name = name,
    host = host,
    agentPort = agentPort,
    sshPort = sshPort,
    authType = authType.name,
    agentToken = agentToken,
    sshUsername = sshUsername,
    sshPassword = sshPassword,
    sshPrivateKey = sshPrivateKey,
    groupName = group
)
