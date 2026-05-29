package com.servercontrol.terminal

import java.util.UUID

data class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val serverId: Long
)

enum class SessionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}
