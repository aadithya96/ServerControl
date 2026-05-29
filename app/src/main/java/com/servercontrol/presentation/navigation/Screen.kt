package com.servercontrol.presentation.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object ServerList : Screen("server_list")
    data object Settings : Screen("settings")
    data object AddServer : Screen("add_server?serverId={serverId}") {
        fun createRoute(serverId: Long? = null) =
            if (serverId != null) "add_server?serverId=$serverId" else "add_server"
    }
    data object Dashboard : Screen("dashboard/{serverId}") {
        fun createRoute(serverId: Long) = "dashboard/$serverId"
    }
    data object Processes : Screen("processes/{serverId}") {
        fun createRoute(serverId: Long) = "processes/$serverId"
    }
    data object Firewall : Screen("firewall/{serverId}") {
        fun createRoute(serverId: Long) = "firewall/$serverId"
    }
    data object Disk : Screen("disk/{serverId}") {
        fun createRoute(serverId: Long) = "disk/$serverId"
    }
    data object Connections : Screen("connections/{serverId}") {
        fun createRoute(serverId: Long) = "connections/$serverId"
    }
}
