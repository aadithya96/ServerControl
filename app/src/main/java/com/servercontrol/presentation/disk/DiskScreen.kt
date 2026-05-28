package com.servercontrol.presentation.disk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.presentation.theme.cpuColor
import com.servercontrol.util.toReadableBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiskScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DiskViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disk & I/O") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.isLoading && state.disks.isEmpty() -> LoadingSpinner()
                state.error != null && state.disks.isEmpty() -> ErrorState(
                    message = state.error!!,
                    onRetry = viewModel::refresh
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.disks, key = { it.mountPoint }) { disk ->
                            DiskCard(disk)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiskCard(disk: DiskInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = disk.mountPoint, style = MaterialTheme.typography.titleSmall)
                AssistChip(
                    onClick = {},
                    label = { Text(disk.fsType, style = MaterialTheme.typography.labelSmall) }
                )
            }
            Text(
                text = disk.device,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { (disk.usedPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = cpuColor(disk.usedPercent)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${disk.usedBytes.toReadableBytes()}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${"%.1f".format(disk.usedPercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = cpuColor(disk.usedPercent)
                )
                Text(
                    text = "Total: ${disk.totalBytes.toReadableBytes()}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (disk.readBytesPerSec > 0 || disk.writeBytesPerSec > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "R: ${disk.readBytesPerSec.toReadableBytes()}/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "W: ${disk.writeBytesPerSec.toReadableBytes()}/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (disk.ioWaitPercent > 0) {
                        Text(
                            text = "I/O Wait: ${"%.1f".format(disk.ioWaitPercent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = cpuColor(disk.ioWaitPercent)
                        )
                    }
                }
            }
        }
    }
}
