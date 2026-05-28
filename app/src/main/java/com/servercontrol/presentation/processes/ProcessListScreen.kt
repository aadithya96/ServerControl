package com.servercontrol.presentation.processes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.ProcessSortOrder
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessListScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProcessListViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()
    var processToKill by remember { mutableStateOf<Process?>(null) }

    val filtered = state.processes.filter {
        state.searchQuery.isEmpty() || it.name.contains(state.searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Processes") },
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
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Search processes") }
                ) {}
                // Sort row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProcessSortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = state.sortOrder == order,
                            onClick = { viewModel.setSortOrder(order) },
                            label = { Text(order.name) }
                        )
                    }
                }
            }
        },
        snackbarHost = {
            state.killResult?.let {
                SnackbarHost(remember { SnackbarHostState() }.also { host ->
                    LaunchedEffect(it) { host.showSnackbar(it) }
                })
            }
        }
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
                    items(filtered, key = { it.pid }) { process ->
                        ProcessRow(
                            process = process,
                            onLongPress = { processToKill = process }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    processToKill?.let { proc ->
        AlertDialog(
            onDismissRequest = { processToKill = null },
            title = { Text("Kill Process") },
            text = { Text("Kill \"${proc.name}\" (PID ${proc.pid})?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.killProcess(proc.pid)
                    processToKill = null
                }) {
                    Text("Kill", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { processToKill = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProcessRow(process: Process, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = process.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "PID ${process.pid} • ${process.user}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "CPU: ${"%.1f".format(process.cpuPercent)}%",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "MEM: ${"%.1f".format(process.memPercent)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
