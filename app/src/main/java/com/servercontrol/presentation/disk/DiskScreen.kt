package com.servercontrol.presentation.disk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.presentation.components.SectionHeader
import com.servercontrol.util.FormatUtils
import com.servercontrol.presentation.theme.CpuCritical
import com.servercontrol.presentation.theme.CpuGood
import com.servercontrol.presentation.theme.CpuWarn

private val REAL_FS_TYPES = setOf("ext2", "ext3", "ext4", "btrfs", "xfs", "zfs", "ntfs", "fat32", "vfat", "exfat", "f2fs", "jfs", "reiserfs", "hfs+", "apfs")
private val VIRTUAL_FS_TYPES = setOf("tmpfs", "devtmpfs", "proc", "sysfs", "cgroup", "cgroup2", "devpts", "mqueue", "debugfs", "securityfs", "pstore", "hugetlbfs", "fusectl", "overlay", "squashfs", "ramfs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiskScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DiskViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()
    var showVirtual by remember { mutableStateOf(false) }

    val realMounts = state.disks.filter { it.fsType.lowercase() in REAL_FS_TYPES }
    val virtualMounts = state.disks.filter {
        it.fsType.lowercase() in VIRTUAL_FS_TYPES || it.fsType.lowercase() !in REAL_FS_TYPES
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disk & I/O") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                        if (realMounts.isNotEmpty()) {
                            item { SectionHeader(title = "Storage") }
                            items(realMounts, key = { it.mountPoint }) { disk ->
                                DiskCard(disk)
                            }
                        }

                        item {
                            // Virtual filesystems toggle
                            OutlinedButton(
                                onClick = { showVirtual = !showVirtual },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (showVirtual) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (showVirtual) "Hide Virtual Filesystems (${virtualMounts.size})"
                                    else "Show Virtual Filesystems (${virtualMounts.size})"
                                )
                            }
                        }

                        if (showVirtual && virtualMounts.isNotEmpty()) {
                            item { SectionHeader(title = "Virtual Filesystems") }
                            items(virtualMounts, key = { "v_${it.mountPoint}" }) { disk ->
                                DiskCard(disk)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiskCard(disk: DiskInfo) {
    val usageColor = when {
        disk.usedPercent >= 90.0 -> CpuCritical
        disk.usedPercent >= 70.0 -> CpuWarn
        else -> CpuGood
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row: mount point + fs type chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = disk.mountPoint,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = disk.device,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(disk.fsType, style = MaterialTheme.typography.labelSmall) }
                )
            }

            // Usage progress bar
            LinearProgressIndicator(
                progress = { (disk.usedPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = usageColor
            )

            // Usage stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used: ${formatBytes(disk.usedBytes)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "${"%.1f".format(disk.usedPercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = usageColor
                )
                Text(
                    text = "Free: ${formatBytes(disk.freeBytes)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Total: ${formatBytes(disk.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // I/O stats
            if (disk.readBytesPerSec > 0 || disk.writeBytesPerSec > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (disk.readBytesPerSec > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("↓ ${formatBytes(disk.readBytesPerSec)}/s", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    if (disk.writeBytesPerSec > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("↑ ${formatBytes(disk.writeBytesPerSec)}/s", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // I/O wait warning
            if (disk.ioWaitPercent > 10.0) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "I/O Wait: ${"%.1f".format(disk.ioWaitPercent)}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }
    }
}

fun formatBytes(bytes: Long): String = FormatUtils.formatBytes(bytes)
