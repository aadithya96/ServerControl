package com.servercontrol.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.servercontrol.presentation.connections.ConnectionsScreen
import com.servercontrol.presentation.dashboard.DashboardScreen
import com.servercontrol.presentation.disk.DiskScreen
import com.servercontrol.presentation.firewall.FirewallScreen
import com.servercontrol.presentation.processes.ProcessListScreen
import com.servercontrol.presentation.servers.AddServerScreen
import com.servercontrol.presentation.servers.ServerListScreen
import com.servercontrol.presentation.settings.SettingsScreen

@Composable
fun NavGraph(startDestination: String = Screen.ServerList.route) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.ServerList.route) {
            ServerListScreen(
                onServerClick = { serverId ->
                    navController.navigate(Screen.Dashboard.createRoute(serverId))
                },
                onAddServer = {
                    navController.navigate(Screen.AddServer.createRoute())
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.AddServer.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            AddServerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            DashboardScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProcesses = { navController.navigate(Screen.Processes.createRoute(serverId)) },
                onNavigateToDisk = { navController.navigate(Screen.Disk.createRoute(serverId)) },
                onNavigateToFirewall = { navController.navigate(Screen.Firewall.createRoute(serverId)) },
                onNavigateToConnections = { navController.navigate(Screen.Connections.createRoute(serverId)) }
            )
        }

        composable(
            route = Screen.Processes.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            ProcessListScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Firewall.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            FirewallScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Disk.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            DiskScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Connections.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            ConnectionsScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
