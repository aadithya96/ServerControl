package com.servercontrol.domain.model

data class SystemService(
    val name: String,
    val description: String,
    val loadState: String,       // loaded/not-found/masked
    val activeState: String,     // active/inactive/failed/activating/deactivating
    val subState: String,        // running/dead/exited/failed/waiting
    val enabled: Boolean,        // enabled/disabled/static
    val unitFilePath: String,
    val execStart: String,
    val type: String             // service/timer/socket/mount/target
)

enum class ServiceAction { START, STOP, RESTART, RELOAD, ENABLE, DISABLE }
