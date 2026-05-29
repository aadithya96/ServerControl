package com.servercontrol.presentation.network

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.BandwidthInfo
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandwidthScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: BandwidthViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bandwidth") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.interfaces.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.interfaces.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::refresh) { Text("Retry") }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.interfaces, key = { it.interfaceName }) { iface ->
                        val ifaceHistory = history[iface.interfaceName] ?: emptyList()
                        BandwidthCard(iface = iface, history = ifaceHistory)
                    }
                }
            }
        }
    }
}

@Composable
private fun BandwidthCard(iface: BandwidthInfo, history: List<Pair<Long, Long>>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(iface.interfaceName, style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↓ Download", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(iface.formatRx(), style = MaterialTheme.typography.bodyLarge)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↑ Upload", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(iface.formatTx(), style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Sparkline chart
            if (history.size > 1) {
                Text("History (30s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BandwidthSparkline(
                    history = history,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                )
            }

            // Total bytes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total RX: ${formatTotalBytes(iface.rxTotalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total TX: ${formatTotalBytes(iface.txTotalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BandwidthSparkline(history: List<Pair<Long, Long>>, modifier: Modifier = Modifier) {
    val rxColor = MaterialTheme.colorScheme.primary
    val txColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        if (history.size < 2) return@Canvas

        val maxRx = history.maxOfOrNull { it.first } ?: 1L
        val maxTx = history.maxOfOrNull { it.second } ?: 1L
        val maxVal = maxOf(maxRx, maxTx, 1L).toFloat()

        val w = size.width
        val h = size.height
        val stepX = w / (history.size - 1).coerceAtLeast(1)

        fun buildPath(values: List<Long>): Path {
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - (v.toFloat() / maxVal * h).coerceIn(0f, h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            return path
        }

        drawPath(buildPath(history.map { it.first }), color = rxColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        drawPath(buildPath(history.map { it.second }), color = txColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun formatTotalBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${"%.1f".format(bytes / 1_073_741_824f)} GB"
    bytes >= 1_048_576L -> "${"%.1f".format(bytes / 1_048_576f)} MB"
    bytes >= 1024L -> "${"%.1f".format(bytes / 1024f)} KB"
    else -> "$bytes B"
}
