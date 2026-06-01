package com.servercontrol.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.servercontrol.presentation.commands.QuickCommandsScreen
import com.servercontrol.presentation.connections.ConnectionsScreen
import com.servercontrol.presentation.disk.DiskScreen
import com.servercontrol.presentation.firewall.FirewallScreen
import com.servercontrol.presentation.metrics.MetricsHistoryScreen
import com.servercontrol.presentation.network.BandwidthScreen
import com.servercontrol.presentation.onboarding.OnboardingScreen
import com.servercontrol.presentation.overview.MultiServerOverviewScreen
import com.servercontrol.presentation.security.SecurityScreen
import com.servercontrol.presentation.servers.AddServerScreen
import com.servercontrol.presentation.servers.AgentInstallerScreen
import com.servercontrol.presentation.servers.QrScanScreen
import com.servercontrol.presentation.servers.QrShareScreen
import com.servercontrol.presentation.servers.ServerListScreen
import com.servercontrol.presentation.services.ServiceManagerScreen
import com.servercontrol.presentation.settings.SettingsViewModel
import com.servercontrol.presentation.terminal.TerminalScreen

@Composable
fun NavGraph() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val onboardingDone by settingsViewModel.uiState.collectAsState()

    val startDestination = if (onboardingDone.onboardingDone) {
        Screen.ServerList.route
    } else {
        Screen.Onboarding.route
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    settingsViewModel.setOnboardingDone(true)
                    navController.navigate(Screen.ServerList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onScanQr = { navController.navigate(Screen.QrScan.route) }
            )
        }

        composable(Screen.ServerList.route) {
            ServerListScreen(
                onServerClick = { serverId ->
                    navController.navigate(Screen.ServerDetail.createRoute(serverId))
                },
                onAddServer = {
                    navController.navigate(Screen.AddServer.createRoute())
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onOpenTerminal = { serverId ->
                    navController.navigate(Screen.Terminal.createRoute(serverId))
                },
                onInstallAgent = { serverId ->
                    navController.navigate(Screen.AgentInstaller.createRoute(serverId))
                },
                onEditServer = { serverId ->
                    navController.navigate(Screen.AddServer.createRoute(serverId))
                },
                onOverview = {
                    navController.navigate(Screen.Overview.route)
                },
                onShareQr = { serverId ->
                    navController.navigate(Screen.QrShare.createRoute(serverId))
                },
                onScanQr = {
                    navController.navigate(Screen.QrScan.route)
                },
                onExportProfiles = { /* handled inside the screen via ProfileExporter */ },
                onImportProfiles = { /* handled inside the screen via file picker */ }
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
            AddServerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInstaller = { serverId ->
                    navController.navigate(Screen.AgentInstaller.createRoute(serverId)) {
                        popUpTo(Screen.AddServer.route) { inclusive = true }
                    }
                }
            )
        }

        // New server-detail container with bottom navigation (Monitor/Processes/Containers/Logs/Settings)
        composable(
            route = Screen.ServerDetail.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            ServerDetailContainer(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTerminal = { navController.navigate(Screen.Terminal.createRoute(serverId)) },
                onNavigateToFirewall = { navController.navigate(Screen.Firewall.createRoute(serverId)) },
                onNavigateToDisk = { navController.navigate(Screen.Disk.createRoute(serverId)) },
                onNavigateToConnections = { navController.navigate(Screen.Connections.createRoute(serverId)) },
                onNavigateToServices = { navController.navigate(Screen.ServiceManager.createRoute(serverId)) },
                onNavigateToMetricsHistory = { navController.navigate(Screen.MetricsHistory.createRoute(serverId)) },
                onNavigateToQuickCommands = { navController.navigate(Screen.QuickCommands.createRoute(serverId)) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.createRoute(serverId)) },
                onNavigateToAgentInstaller = { navController.navigate(Screen.AgentInstaller.createRoute(serverId)) },
            )
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

        composable(
            route = Screen.ServiceManager.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            ServiceManagerScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.MetricsHistory.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            MetricsHistoryScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            com.servercontrol.presentation.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onQrTransfer = { navController.navigate(Screen.QrScan.route) }
            )
        }

        composable(
            route = Screen.Terminal.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            TerminalScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.AgentInstaller.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            AgentInstallerScreen(
                serverId = serverId,
                onNavigateBack = { navController.popBackStack() },
                onAgentConfigured = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QuickCommands.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            QuickCommandsScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Security.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            SecurityScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Overview.route) {
            MultiServerOverviewScreen(
                onNavigateToServer = { serverId ->
                    navController.navigate(Screen.ServerDetail.createRoute(serverId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QrShare.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            QrShareScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.QrScan.route) {
            QrScanScreen(
                onServerScanned = { _ ->
                    navController.navigate(Screen.AddServer.createRoute()) {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Bandwidth.route,
            arguments = listOf(navArgument("serverId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serverId = backStackEntry.arguments?.getLong("serverId") ?: return@composable
            BandwidthScreen(serverId = serverId, onNavigateBack = { navController.popBackStack() })
        }
    }
}
