package com.servercontrol.presentation.processes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.ProcessSortOrder
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.presentation.theme.cpuColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessListScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProcessListViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()
    var processForBottomSheet by remember { mutableStateOf<Process?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    LaunchedEffect(state.killResult) {
        state.killResult?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Processes")
                            if (state.processes.isNotEmpty()) {
                                Text(
                                    text = "${state.processes.size} processes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Switch(
                            checked = state.autoRefresh,
                            onCheckedChange = viewModel::setAutoRefresh
                        )
                    }
                )
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search processes") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
                // Sort chips
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortOptions = listOf(
                        ProcessSortOrder.CPU to "CPU%",
                        ProcessSortOrder.MEMORY to "MEM%",
                        ProcessSortOrder.PID to "PID",
                        ProcessSortOrder.NAME to "Name"
                    )
                    items(sortOptions) { (order, label) ->
                        FilterChip(
                            selected = sortBy == order,
                            onClick = { viewModel.setSortOrder(order) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> LoadingSpinner(modifier = Modifier.padding(padding))
            state.error != null -> ErrorState(
                message = state.error!!,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.processes, key = { it.pid }) { process ->
                        ProcessRow(
                            process = process,
                            onLongPress = { processForBottomSheet = process }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    processForBottomSheet?.let { proc ->
        ProcessBottomSheet(
            process = proc,
            onDismiss = { processForBottomSheet = null },
            onKill = {
                viewModel.killProcess(proc.pid)
                processForBottomSheet = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProcessRow(process: Process, onLongPress: () -> Unit) {
    val cpuColor = cpuColor(process.cpuPercent)
    val initial = process.name.firstOrNull()?.uppercaseChar() ?: '?'

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circle avatar with first letter, colored by cpu%
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(cpuColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = cpuColor,
                fontWeight = FontWeight.Bold
            )
        }

        // Name + PID/user
        Column(modifier = Modifier.weight(1f)) {
            Text(text = process.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "PID ${process.pid} · ${process.user}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CPU and MEM bars
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${"%.1f".format(process.cpuPercent)}%",
                    style = MaterialTheme.typography.labelSmall
                )
                LinearProgressIndicator(
                    progress = { (process.cpuPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.width(40.dp).height(4.dp),
                    color = cpuColor(process.cpuPercent)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${"%.1f".format(process.memPercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { (process.memPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.width(40.dp).height(4.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessBottomSheet(
    process: Process,
    onDismiss: () -> Unit,
    onKill: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = process.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            DetailRow("PID", process.pid.toString())
            DetailRow("User", process.user)
            DetailRow("Status", process.status)
            DetailRow("CPU", "${"%.2f".format(process.cpuPercent)}%")
            DetailRow("Memory", "${"%.2f".format(process.memPercent)}% (${formatBytes(process.memRss)})")
            if (process.command.isNotBlank()) {
                Text(
                    text = process.command,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onKill,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Kill Process", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.1f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}
