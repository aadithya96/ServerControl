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
    data object Terminal : Screen("terminal/{serverId}") {
        fun createRoute(serverId: Long) = "terminal/$serverId"
    }
    data object AgentInstaller : Screen("agent_installer/{serverId}") {
        fun createRoute(serverId: Long) = "agent_installer/$serverId"
    }
    data object ServiceManager : Screen("service_manager/{serverId}") {
        fun createRoute(serverId: Long) = "service_manager/$serverId"
    }
    data object LogViewer : Screen("log_viewer/{serverId}") {
        fun createRoute(serverId: Long) = "log_viewer/$serverId"
    }
    data object MetricsHistory : Screen("metrics_history/{serverId}") {
        fun createRoute(serverId: Long) = "metrics_history/$serverId"
    }
    data object Docker : Screen("docker/{serverId}") {
        fun createRoute(serverId: Long) = "docker/$serverId"
    }
    data object QuickCommands : Screen("quick_commands/{serverId}") {
        fun createRoute(serverId: Long) = "quick_commands/$serverId"
    }
    data object Security : Screen("security/{serverId}") {
        fun createRoute(serverId: Long) = "security/$serverId"
    }
}
