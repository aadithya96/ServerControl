package com.servercontrol.presentation.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.presentation.theme.CpuCritical
import com.servercontrol.presentation.theme.CpuGood
import com.servercontrol.presentation.theme.CpuWarn
import com.servercontrol.presentation.theme.cpuColor
import com.servercontrol.util.Resource
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiServerOverviewScreen(
    onNavigateToServer: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MultiServerOverviewViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    val serverStats by viewModel.serverStats.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val compareMode by viewModel.compareMode.collectAsState()
    val compareServerIds by viewModel.compareServerIds.collectAsState()

    val filteredServers = if (selectedGroup == null) servers
    else servers.filter { it.group == selectedGroup }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overview") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleCompareMode) {
                        Icon(
                            if (compareMode) Icons.Default.Checklist else Icons.Default.CompareArrows,
                            contentDescription = if (compareMode) "Exit compare" else "Compare servers",
                            tint = if (compareMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Group filter chips
            if (groups.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedGroup == null,
                            onClick = { viewModel.selectGroup(null) },
                            label = { Text("All") }
                        )
                    }
                    items(groups) { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = { viewModel.selectGroup(group) },
                            label = { Text(group.replaceFirstChar { it.uppercaseChar() }) }
                        )
                    }
                }
            }

            if (!compareMode) {
                // Grid of server cards
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        val stats = serverStats[server.id]
                        ServerOverviewCard(
                            server = server,
                            stats = stats,
                            onClick = { onNavigateToServer(server.id) }
                        )
                    }
                }
            } else {
                // Compare mode
                Text(
                    text = "Select 2 servers to compare",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        val stats = serverStats[server.id]
                        val isSelected = server.id in compareServerIds
                        ServerOverviewCard(
                            server = server,
                            stats = stats,
                            onClick = { viewModel.toggleCompareServer(server.id) },
                            isSelected = isSelected,
                            showCheckbox = true
                        )
                    }
                }

                // Compare panel
                if (compareServerIds.size == 2) {
                    val serverA = servers.find { it.id == compareServerIds[0] }
                    val serverB = servers.find { it.id == compareServerIds[1] }
                    val statsA = (serverStats[compareServerIds[0]] as? Resource.Success)?.data
                    val statsB = (serverStats[compareServerIds[1]] as? Resource.Success)?.data

                    if (serverA != null && serverB != null) {
                        ComparePanelCard(
                            serverA = serverA, statsA = statsA,
                            serverB = serverB, statsB = statsB,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerOverviewCard(
    server: ServerProfile,
    stats: Resource<SystemStats>?,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    showCheckbox: Boolean = false
) {
    val statsData = (stats as? Resource.Success)?.data
    val isOnline = stats is Resource.Success
    val cpuPercent = statsData?.cpuPercent ?: 0.0
    val memPercent = statsData?.memPercent ?: 0.0
    val diskPercent = 0.0 // Would need disk data — use placeholder

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = server.host,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (showCheckbox) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) CpuGood else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }

            // CPU arc gauge (mini)
            if (isOnline && statsData != null) {
                Box(modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally)) {
                    MiniArcGauge(percent = cpuPercent)
                }
            } else {
                Box(modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (stats is Resource.Loading) "…" else "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // RAM bar
            if (statsData != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("RAM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { (memPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = cpuColor(memPercent)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniArcGauge(percent: Double) {
    val color = cpuColor(percent)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 6.dp.toPx()
        val inset = strokeWidth / 2f
        val arcRect = androidx.compose.ui.geometry.Rect(inset, inset, size.width - inset, size.height - inset)
        // Background track
        drawArc(
            color = trackColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = Size(arcRect.width, arcRect.height),
            style = Stroke(width = strokeWidth)
        )
        // Progress arc
        drawArc(
            color = color,
            startAngle = 135f,
            sweepAngle = (270f * (percent / 100.0).toFloat()).coerceIn(0f, 270f),
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = Size(arcRect.width, arcRect.height),
            style = Stroke(width = strokeWidth)
        )
        // Center text - draw manually
        val text = "${"%.0f".format(percent)}%"
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 10.dp.toPx()
                this.color = android.graphics.Color.WHITE
            }
            drawText(text, size.width / 2f, size.height / 2f + paint.textSize / 3f, paint)
        }
    }
}

@Composable
private fun ComparePanelCard(
    serverA: ServerProfile,
    statsA: SystemStats?,
    serverB: ServerProfile,
    statsB: SystemStats?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Header row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(modifier = Modifier.width(60.dp))
                Text(serverA.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), maxLines = 1)
                Text(serverB.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), maxLines = 1)
            }

            HorizontalDivider()

            // CPU
            CompareRow(
                label = "CPU%",
                valueA = "${"%.1f".format(statsA?.cpuPercent ?: 0.0)}%",
                valueB = "${"%.1f".format(statsB?.cpuPercent ?: 0.0)}%",
                progressA = (statsA?.cpuPercent?.toFloat() ?: 0f) / 100f,
                progressB = (statsB?.cpuPercent?.toFloat() ?: 0f) / 100f,
                colorA = cpuColor(statsA?.cpuPercent ?: 0.0),
                colorB = cpuColor(statsB?.cpuPercent ?: 0.0)
            )

            // RAM
            CompareRow(
                label = "RAM%",
                valueA = "${"%.1f".format(statsA?.memPercent ?: 0.0)}%",
                valueB = "${"%.1f".format(statsB?.memPercent ?: 0.0)}%",
                progressA = (statsA?.memPercent?.toFloat() ?: 0f) / 100f,
                progressB = (statsB?.memPercent?.toFloat() ?: 0f) / 100f,
                colorA = cpuColor(statsA?.memPercent ?: 0.0),
                colorB = cpuColor(statsB?.memPercent ?: 0.0)
            )

            // Uptime
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Uptime", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp))
                Text(
                    text = formatUptime(statsA?.uptimeSeconds ?: 0),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatUptime(statsB?.uptimeSeconds ?: 0),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    valueA: String,
    valueB: String,
    progressA: Float,
    progressB: Float,
    colorA: Color,
    colorB: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp))
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progressA.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = colorA
                )
                Text(valueA, style = MaterialTheme.typography.bodySmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progressB.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = colorB
                )
                Text(valueB, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
