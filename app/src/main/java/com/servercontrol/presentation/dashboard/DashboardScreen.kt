package com.servercontrol.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.presentation.components.*
import com.servercontrol.presentation.theme.cpuColor
import com.servercontrol.util.toReadableUptime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToProcesses: () -> Unit,
    onNavigateToDisk: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToConnections: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showIntervalMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showIntervalMenu = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh interval")
                    }
                    DropdownMenu(
                        expanded = showIntervalMenu,
                        onDismissRequest = { showIntervalMenu = false }
                    ) {
                        listOf(2, 5, 10, 30).forEach { secs ->
                            DropdownMenuItem(
                                text = { Text("Every ${secs}s") },
                                onClick = {
                                    viewModel.setRefreshInterval(secs)
                                    showIntervalMenu = false
                                },
                                leadingIcon = if (state.refreshIntervalSeconds == secs) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.stats == null -> LoadingSpinner()
                state.error != null && state.stats == null -> ErrorState(
                    message = state.error!!,
                    onRetry = viewModel::refresh
                )
                else -> {
                    state.stats?.let { stats ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // CPU Gauge
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    GaugeChart(
                                        percent = stats.cpuPercent,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Text(
                                        text = "CPU Usage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // RAM & Swap
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    UsageBar(
                                        label = "RAM",
                                        usedBytes = stats.memUsedBytes,
                                        totalBytes = stats.memTotalBytes,
                                        percent = stats.memPercent,
                                        color = cpuColor(stats.memPercent)
                                    )
                                    UsageBar(
                                        label = "Swap",
                                        usedBytes = stats.swapUsedBytes,
                                        totalBytes = stats.swapTotalBytes,
                                        percent = stats.swapPercent,
                                        color = cpuColor(stats.swapPercent)
                                    )
                                }
                            }

                            // Load average & uptime
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(
                                    title = "Load 1m",
                                    value = "%.2f".format(stats.loadAvg1m),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "Load 5m",
                                    value = "%.2f".format(stats.loadAvg5m),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    title = "Load 15m",
                                    value = "%.2f".format(stats.loadAvg15m),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            AssistChip(
                                onClick = {},
                                label = { Text("Uptime: ${stats.uptimeSeconds.toReadableUptime()}") },
                                leadingIcon = {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )

                            // Navigation shortcuts
                            SectionHeader(title = "Monitor")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onNavigateToProcesses,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Processes") }
                                FilledTonalButton(
                                    onClick = onNavigateToDisk,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Disk") }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onNavigateToFirewall,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Firewall") }
                                FilledTonalButton(
                                    onClick = onNavigateToConnections,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Connections") }
                            }
                        }
                    }
                }
            }
        }
    }
}
