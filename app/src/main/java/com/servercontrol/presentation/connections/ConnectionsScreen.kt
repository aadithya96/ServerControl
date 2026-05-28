package com.servercontrol.presentation.connections

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
import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.presentation.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()

    val filtered = state.connections.filter { conn ->
        (state.protocolFilter == "ALL" || conn.protocol.equals(state.protocolFilter, ignoreCase = true)) &&
        (state.stateFilter == "ALL" || conn.state == state.stateFilter) &&
        (state.searchQuery.isEmpty() ||
            conn.localAddress.contains(state.searchQuery) ||
            conn.remoteAddress.contains(state.searchQuery))
    }

    val grouped = filtered.groupBy { it.state }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Protocol filter
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "TCP", "UDP").forEach { proto ->
                    FilterChip(
                        selected = state.protocolFilter == proto,
                        onClick = { viewModel.setProtocolFilter(proto) },
                        label = { Text(proto) }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.connections.isEmpty() -> LoadingSpinner()
                    state.error != null && state.connections.isEmpty() -> ErrorState(
                        message = state.error!!,
                        onRetry = viewModel::refresh
                    )
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            grouped.forEach { (connState, conns) ->
                                item { SectionHeader(title = "$connState (${conns.size})") }
                                items(conns) { conn ->
                                    ConnectionRow(conn)
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionRow(conn: NetworkConnection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${conn.protocol.uppercase()} ${conn.localAddress}:${conn.localPort}",
                style = MaterialTheme.typography.labelMedium
            )
            AssistChip(
                onClick = {},
                label = { Text(conn.state, style = MaterialTheme.typography.labelSmall) }
            )
        }
        Text(
            text = "→ ${conn.remoteAddress}:${conn.remotePort}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        conn.processName?.let {
            Text(
                text = "Process: $it (PID ${conn.pid})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
