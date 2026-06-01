package com.servercontrol.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.servercontrol.presentation.dashboard.DashboardScreen
import com.servercontrol.presentation.docker.DockerScreen
import com.servercontrol.presentation.logs.LogViewerScreen
import com.servercontrol.presentation.processes.ProcessListScreen
import com.servercontrol.presentation.settings.SettingsScreen
import androidx.compose.foundation.layout.padding

private data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val tabs = listOf(
    NavTab("monitor",    "Monitor",    Icons.Outlined.Analytics,  Icons.Filled.Analytics),
    NavTab("processes",  "Processes",  Icons.Outlined.Memory,     Icons.Filled.Memory),
    NavTab("containers", "Containers", Icons.Outlined.ViewInAr,   Icons.Filled.ViewInAr),
    NavTab("logs",       "Logs",       Icons.Outlined.Description, Icons.Filled.Description),
    NavTab("settings",   "Settings",   Icons.Outlined.Settings,   Icons.Filled.Settings),
)

@Composable
fun ServerDetailContainer(
    serverId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToTerminal: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToDisk: () -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToMetricsHistory: () -> Unit,
    onNavigateToQuickCommands: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAgentInstaller: () -> Unit,
) {
    val innerNav = rememberNavController()
    val backEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route ?: "monitor"

    fun switchTab(route: String) {
        innerNav.navigate(route) {
            popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { switchTab(tab.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNav,
            startDestination = "monitor",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("monitor") {
                DashboardScreen(
                    serverId = serverId,
                    onNavigateBack = onNavigateBack,
                    onNavigateToProcesses = { switchTab("processes") },
                    onNavigateToDisk = onNavigateToDisk,
                    onNavigateToFirewall = onNavigateToFirewall,
                    onNavigateToConnections = onNavigateToConnections,
                    onNavigateToTerminal = onNavigateToTerminal,
                    onNavigateToAgentInstaller = onNavigateToAgentInstaller,
                    onNavigateToServices = onNavigateToServices,
                    onNavigateToLogs = { switchTab("logs") },
                    onNavigateToMetricsHistory = onNavigateToMetricsHistory,
                    onNavigateToDocker = { switchTab("containers") },
                    onNavigateToQuickCommands = onNavigateToQuickCommands,
                    onNavigateToSecurity = onNavigateToSecurity
                )
            }
            composable("processes") {
                ProcessListScreen(
                    serverId = serverId,
                    onNavigateBack = { switchTab("monitor") }
                )
            }
            composable("containers") {
                DockerScreen(
                    serverId = serverId,
                    onNavigateBack = { switchTab("monitor") }
                )
            }
            composable("logs") {
                LogViewerScreen(
                    serverId = serverId,
                    onNavigateBack = { switchTab("monitor") }
                )
            }
            composable("settings") {
                SettingsScreen(onNavigateBack = { switchTab("monitor") })
            }
        }
    }
}
