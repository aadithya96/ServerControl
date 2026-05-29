package com.servercontrol.domain.model

data class SavedCommand(
    val id: String,
    val serverId: String?,
    val name: String,
    val command: String,
    val description: String,
    val isBuiltIn: Boolean
)
