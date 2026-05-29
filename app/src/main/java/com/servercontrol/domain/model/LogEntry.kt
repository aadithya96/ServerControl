package com.servercontrol.domain.model

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val source: String,
    val message: String,
    val rawLine: String
)

enum class LogLevel { ERROR, WARN, INFO, DEBUG, UNKNOWN }

data class LogSource(
    val id: String,
    val displayName: String,
    val command: String
)
