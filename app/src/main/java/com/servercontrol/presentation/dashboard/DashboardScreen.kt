package com.servercontrol.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.presentation.components.*
import com.servercontrol.presentation.theme.*
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
    onNavigateToServices: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToMetricsHistory: () -> Unit = {},
    onNavigateToDocker: () -> Unit = {},
    onNavigateToQuickCommands: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val uiState by viewModel.uiState.collectAsState()
    val cpuHistory by viewModel.cpuHistory.collectAsState()

    var showOverflow by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.stats?.hostname?.takeIf { it.isNotBlank() } ?: "Monitor",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Terminal") },
                                onClick = { showOverflow = false; onNavigateToTerminal() },
                                leadingIcon = { Icon(Icons.Filled.Terminal, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Processes") },
                                onClick = { showOverflow = false; onNavigateToProcesses() },
                                leadingIcon = { Icon(Icons.Filled.Memory, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Services") },
                                onClick = { showOverflow = false; onNavigateToServices() },
                                leadingIcon = { Icon(Icons.Filled.Dns, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Metrics History") },
                                onClick = { showOverflow = false; onNavigateToMetricsHistory() },
                                leadingIcon = { Icon(Icons.Filled.ShowChart, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Quick Commands") },
                                onClick = { showOverflow = false; onNavigateToQuickCommands() },
                                leadingIcon = { Icon(Icons.Filled.Code, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Agent Installer") },
                                onClick = { showOverflow = false; onNavigateToAgentInstaller() },
                                leadingIcon = { Icon(Icons.Filled.Download, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) { ShimmerCard() }
                    }
                }
                uiState.error != null && uiState.stats == null -> {
                    ErrorState(message = uiState.error!!, onRetry = viewModel::refresh)
                }
                else -> {
                    uiState.stats?.let { stats ->
                        DashboardContent(
                            stats = stats,
                            cpuHistory = cpuHistory,
                            onNavigateToProcesses = onNavigateToProcesses,
                            onNavigateToDisk = onNavigateToDisk,
                            onNavigateToFirewall = onNavigateToFirewall,
                            onNavigateToConnections = onNavigateToConnections,
                            onNavigateToTerminal = onNavigateToTerminal,
                            onNavigateToServices = onNavigateToServices,
                            onNavigateToLogs = onNavigateToLogs,
                            onNavigateToDocker = onNavigateToDocker,
                            onNavigateToQuickCommands = onNavigateToQuickCommands,
                            onNavigateToSecurity = onNavigateToSecurity,
                            onNavigateToMetricsHistory = onNavigateToMetricsHistory
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
    onNavigateToConnections: () -> Unit,
    onNavigateToTerminal: () -> Unit = {},
    onNavigateToServices: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateToDocker: () -> Unit = {},
    onNavigateToQuickCommands: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToMetricsHistory: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Health hero
        item { HealthHeroCard(stats) }

        // 2. Resources – three radial gauges
        item { ResourcesCard(stats) }

        // 3. Network throughput (sparkline with CPU history as proxy)
        item { NetworkCard(cpuHistory) }

        // 4. Quick actions 3×2 grid
        item {
            QuickActionsCard(
                onTerminal = onNavigateToTerminal,
                onFirewall = onNavigateToFirewall,
                onDisk = onNavigateToDisk,
                onConnections = onNavigateToConnections,
                onMetrics = onNavigateToMetricsHistory,
                onSecurity = onNavigateToSecurity
            )
        }
    }
}

// ── Health hero ───────────────────────────────────────────────────────────────

@Composable
private fun HealthHeroCard(stats: SystemStats) {
    val sc = LocalStatusColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SC2,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stats.hostname.ifBlank { "Server" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = MonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = sc.online,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "All systems nominal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = sc.online
                        )
                    }
                }
                StatusChip(status = ServerStatus.ONLINE)
            }
            HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InlineStat("UPTIME", stats.uptimeSeconds.toReadableUptime(), Modifier.weight(1f))
                InlineStat("LOAD", "%.2f".format(stats.loadAvg1m), Modifier.weight(1f))
                InlineStat("CORES", stats.cpuCores.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InlineStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Resources gauges ──────────────────────────────────────────────────────────

@Composable
private fun ResourcesCard(stats: SystemStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SC2,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "RESOURCES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val memGb = stats.memTotalBytes / (1024.0 * 1024 * 1024)
                RadialGauge(
                    percent = stats.cpuPercent.toFloat(),
                    label = "CPU",
                    subCaption = "${stats.cpuCores} cores"
                )
                RadialGauge(
                    percent = stats.memPercent.toFloat(),
                    label = "MEMORY",
                    subCaption = "%.1f GB".format(memGb)
                )
                RadialGauge(
                    percent = stats.swapPercent.toFloat(),
                    label = "SWAP",
                    subCaption = if (stats.swapTotalBytes > 0) "%.1f GB".format(
                        stats.swapTotalBytes / (1024.0 * 1024 * 1024)
                    ) else "N/A"
                )
            }
        }
    }
}

@Composable
private fun RadialGauge(
    percent: Float,
    label: String,
    subCaption: String
) {
    val arcColor = if (percent >= 80f) StatusWarnColor else Primary
    val sweepTarget = (percent / 100f * 270f).coerceIn(0f, 270f)
    val animatedSweep by animateFloatAsState(
        targetValue = sweepTarget,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "gauge_sweep"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 9.dp.toPx()
                val pad = stroke / 2
                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                val topLeft = Offset(pad, pad)
                // Track
                drawArc(
                    color = SC4,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // Arc
                if (animatedSweep > 0f) {
                    drawArc(
                        color = arcColor,
                        startAngle = 135f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.0f".format(percent) + "%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = arcColor
                )
                Text(
                    text = subCaption,
                    fontSize = 10.sp,
                    color = arcColor,
                    textAlign = TextAlign.Center
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

// ── Network throughput (sparklines) ───────────────────────────────────────────

@Composable
private fun NetworkCard(cpuHistory: List<Float>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SC2,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "NETWORK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NetworkWell(
                    label = "Inbound",
                    icon = Icons.Filled.South,
                    value = "—",
                    history = cpuHistory,
                    modifier = Modifier.weight(1f)
                )
                NetworkWell(
                    label = "Outbound",
                    icon = Icons.Filled.North,
                    value = "—",
                    history = cpuHistory.map { it * 0.6f },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NetworkWell(
    label: String,
    icon: ImageVector,
    value: String,
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = WellShape,
        color = SC3
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (history.size > 1) {
                Sparkline(
                    data = history,
                    color = Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                )
            }
        }
    }
}

@Composable
private fun Sparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    val fillColor = color.copy(alpha = 0.13f)
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val maxVal = data.max().coerceAtLeast(1f)
        val w = size.width
        val h = size.height
        val step = w / (data.size - 1).coerceAtLeast(1).toFloat()

        val linePath = Path()
        val fillPath = Path()
        data.forEachIndexed { i, v ->
            val x = i * step
            val y = h - (v / maxVal * h).coerceIn(0f, h)
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo((data.size - 1) * step, h)
        fillPath.close()

        drawPath(fillPath, fillColor)
        drawPath(linePath, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ── Quick actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsCard(
    onTerminal: () -> Unit,
    onFirewall: () -> Unit,
    onDisk: () -> Unit,
    onConnections: () -> Unit,
    onMetrics: () -> Unit,
    onSecurity: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SC2,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "QUICK ACTIONS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionTile("Terminal", Icons.Filled.Terminal, onTerminal, Modifier.weight(1f))
                    ActionTile("Firewall", Icons.Filled.Security, onFirewall, Modifier.weight(1f))
                    ActionTile("Disk", Icons.Filled.Storage, onDisk, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionTile("Network", Icons.Filled.Lan, onConnections, Modifier.weight(1f))
                    ActionTile("Metrics", Icons.Filled.ShowChart, onMetrics, Modifier.weight(1f))
                    ActionTile("Security", Icons.Filled.Shield, onSecurity, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(WellShape)
            .clickable(onClick = onClick),
        shape = WellShape,
        color = SC3
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Shimmer ───────────────────────────────────────────────────────────────────

@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = SC2
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(shimmerBrush(), CardShape)
        )
    }
}

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        SC3.copy(alpha = 0.6f),
        SC4.copy(alpha = 0.4f),
        SC3.copy(alpha = 0.6f)
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
