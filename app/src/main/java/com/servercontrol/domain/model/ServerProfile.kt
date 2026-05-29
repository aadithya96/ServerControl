package com.servercontrol.domain.model

data class ServerProfile(
    val id: Long = 0,
    val name: String,
    val host: String,
    val agentPort: Int = 9876,
    val sshPort: Int = 22,
    val authType: AuthType,
    val agentToken: String? = null,
    val sshUsername: String? = null,
    val sshPassword: String? = null,
    val sshPrivateKey: String? = null,
    val isOnline: Boolean = false,
    val group: String = "default"
)

enum class AuthType {
    AGENT_TOKEN,
    SSH_PASSWORD,
    SSH_KEY
}
