package com.servercontrol.presentation.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.presentation.components.*
import com.servercontrol.presentation.theme.cpuColor
import com.servercontrol.util.Resource
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
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToAgentInstaller: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val uiState by viewModel.uiState.collectAsState()
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    val refreshInterval by viewModel.refreshInterval.collectAsState()
    val autoRefresh by viewModel.autoRefresh.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val hostname = uiState.stats?.hostname?.takeIf { it.isNotBlank() } ?: "Dashboard"
                    Text(hostname)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToTerminal) {
                        Icon(Icons.Default.Terminal, contentDescription = "Open Terminal")
                    }
                    IconButton(onClick = viewModel::toggleAutoRefresh) {
                        Icon(
                            if (autoRefresh) Icons.Default.Sync else Icons.Default.SyncDisabled,
                            contentDescription = if (autoRefresh) "Disable auto-refresh" else "Enable auto-refresh"
                        )
                    }
                    var showIntervalMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showIntervalMenu = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Refresh interval")
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
                                leadingIcon = if (refreshInterval == secs) {
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
            isRefreshing = uiState.isLoading && uiState.stats != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading && uiState.stats == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(4) { ShimmerCard() }
                    }
                }
                uiState.error != null && uiState.stats == null -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = viewModel::refresh
                    )
                }
                else -> {
                    uiState.stats?.let { stats ->
                        DashboardContent(
                            stats = stats,
                            cpuHistory = cpuHistory,
                            onNavigateToProcesses = onNavigateToProcesses,
                            onNavigateToDisk = onNavigateToDisk,
                            onNavigateToFirewall = onNavigateToFirewall,
                            onNavigateToConnections = onNavigateToConnections
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    stats: SystemStats,
    cpuHistory: List<Float>,
    onNavigateToProcesses: () -> Unit,
    onNavigateToDisk: () -> Unit,
    onNavigateToFirewall: () -> Unit,
    onNavigateToConnections: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CPU Gauge + history
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GaugeChart(
                    percent = stats.cpuPercent,
                    size = 200.dp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "CPU Usage",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (cpuHistory.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CpuSparkline(
                        history = cpuHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }
            }
        }

        // RAM
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
                if (stats.swapTotalBytes > 0) {
                    UsageBar(
                        label = "Swap",
                        usedBytes = stats.swapUsedBytes,
                        totalBytes = stats.swapTotalBytes,
                        percent = stats.swapPercent,
                        color = cpuColor(stats.swapPercent)
                    )
                }
            }
        }

        // Load average
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(title = "1 min", value = "%.2f".format(stats.loadAvg1m), modifier = Modifier.weight(1f))
            StatCard(title = "5 min", value = "%.2f".format(stats.loadAvg5m), modifier = Modifier.weight(1f))
            StatCard(title = "15 min", value = "%.2f".format(stats.loadAvg15m), modifier = Modifier.weight(1f))
        }

        // Uptime
        AssistChip(
            onClick = {},
            label = { Text("Uptime: ${stats.uptimeSeconds.toReadableUptime()}") },
            leadingIcon = {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )

        // Navigation chips
        SectionHeader(title = "Monitor")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = onNavigateToProcesses,
                modifier = Modifier.weight(1f)
            ) { Text("Processes") }
            ElevatedButton(
                onClick = onNavigateToDisk,
                modifier = Modifier.weight(1f)
            ) { Text("Disk") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = onNavigateToConnections,
                modifier = Modifier.weight(1f)
            ) { Text("Connections") }
            ElevatedButton(
                onClick = onNavigateToFirewall,
                modifier = Modifier.weight(1f)
            ) { Text("Firewall") }
        }
    }
}

@Composable
private fun CpuSparkline(history: List<Float>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas
        val maxVal = 100f
        val w = size.width
        val h = size.height
        val stepX = w / (history.size - 1).coerceAtLeast(1)

        val path = Path()
        history.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / maxVal * h).coerceIn(0f, h)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val shimmerBrush = shimmerBrush()
    Card(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(shimmerBrush, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}
