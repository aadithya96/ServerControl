package com.servercontrol.presentation.metrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.MetricSample
import com.servercontrol.presentation.components.LineChart
import com.servercontrol.presentation.components.SectionHeader
import com.servercontrol.presentation.dashboard.ShimmerCard
import com.servercontrol.presentation.theme.CpuCritical
import com.servercontrol.presentation.theme.CpuGood
import com.servercontrol.presentation.theme.CpuWarn
import com.servercontrol.presentation.theme.cpuColor
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsHistoryScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MetricsHistoryViewModel = hiltViewModel()
) {
    val samples by viewModel.samples.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val context = LocalContext.current

    val sampleList = (samples as? Resource.Success)?.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metrics History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.exportCsv(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
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
            // Time range chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ranges = listOf(1 to "1h", 6 to "6h", 24 to "24h", 168 to "7d")
                items(ranges) { (hours, label) ->
                    FilterChip(
                        selected = timeRange == hours,
                        onClick = { viewModel.setTimeRange(hours) },
                        label = { Text(label) }
                    )
                }
            }

            when (val s = samples) {
                is Resource.Loading -> {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { ShimmerCard() }
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = viewModel::refresh) { Text("Retry") }
                        }
                    }
                }
                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No data yet — metrics are collected in the background",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Metrics are sampled automatically every 15 minutes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        MetricsContent(samples = s.data)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsContent(samples: List<MetricSample>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val cpuData = samples.map { it.cpuPercent }
        val memData = samples.map {
            if (it.memTotalBytes > 0) it.memUsedBytes.toFloat() / it.memTotalBytes * 100f else 0f
        }
        val diskData = samples.map {
            if (it.diskTotalBytes > 0) it.diskUsedBytes.toFloat() / it.diskTotalBytes * 100f else 0f
        }

        val avgCpu = if (cpuData.isNotEmpty()) cpuData.average().toDouble() else 0.0

        // CPU
        SectionHeader("CPU Usage")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                LineChart(
                    data = cpuData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    lineColor = cpuColor(avgCpu)
                )
                Spacer(modifier = Modifier.height(8.dp))
                MetricStatsRow(data = cpuData, label = "CPU", unit = "%")
            }
        }

        // Memory
        SectionHeader("Memory Usage")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                LineChart(
                    data = memData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    lineColor = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(8.dp))
                MetricStatsRow(data = memData, label = "RAM", unit = "%")

                // Also show absolute values for last sample
                samples.lastOrNull()?.let { last ->
                    if (last.memTotalBytes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Current: ${last.memUsedBytes.toReadableBytes()} / ${last.memTotalBytes.toReadableBytes()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Disk
        SectionHeader("Disk Usage")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                LineChart(
                    data = diskData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    lineColor = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(8.dp))
                MetricStatsRow(data = diskData, label = "Disk", unit = "%")
            }
        }

        Text(
            "${samples.size} samples",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun MetricStatsRow(data: List<Float>, label: String, unit: String) {
    if (data.isEmpty()) return
    val min = data.min()
    val max = data.max()
    val avg = data.average().toFloat()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        listOf("Min" to min, "Avg" to avg, "Max" to max).forEach { (title, value) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "%.1f%s".format(value, unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cpuColor(value.toDouble())
                )
            }
        }
    }
}

private fun Long.toReadableBytes(): String {
    val gb = this / (1024.0 * 1024 * 1024)
    val mb = this / (1024.0 * 1024)
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        else -> "$this B"
    }
}
