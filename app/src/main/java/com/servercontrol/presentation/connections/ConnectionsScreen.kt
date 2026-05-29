package com.servercontrol.presentation.connections

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel()
) {
    val connectionsResource by viewModel.connections.collectAsState()
    val protoFilter by viewModel.protoFilter.collectAsState()
    val stateFilter by viewModel.stateFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val connectionCount = (connectionsResource as? Resource.Success)?.data?.size ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Connections")
                        if (connectionCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = connectionCount.toString(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Protocol filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "tcp", "udp").forEach { proto ->
                    FilterChip(
                        selected = protoFilter == proto,
                        onClick = { viewModel.setProtoFilter(proto) },
                        label = { Text(proto.uppercase()) }
                    )
                }
            }

            // State filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "ESTABLISHED", "LISTEN", "TIME_WAIT", "CLOSE_WAIT").forEach { s ->
                    FilterChip(
                        selected = stateFilter == s,
                        onClick = { viewModel.setStateFilter(s) },
                        label = { Text(if (s == "all") "All" else s.replace("_", " ")) }
                    )
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search by address or process…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            when (val resource = connectionsResource) {
                is Resource.Loading -> LoadingSpinner()
                is Resource.Error -> ErrorState(message = resource.message, onRetry = viewModel::refresh)
                is Resource.Success -> {
                    val grouped = resource.data.groupBy { it.state }.toSortedMap()
                    if (resource.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No connections match filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            grouped.forEach { (connState, conns) ->
                                stickyHeader {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$connState (${conns.size})",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Protocol chip
        val protoColor = if (conn.protocol.equals("tcp", ignoreCase = true))
            Color(0xFF1565C0) else Color(0xFFE65100)
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = protoColor
        ) {
            Text(
                text = conn.protocol.uppercase(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${conn.localAddress}:${conn.localPort}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${conn.remoteAddress}:${conn.remotePort}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (conn.processName != null || conn.pid != null) {
                val procText = buildString {
                    conn.processName?.let { append(it) }
                    conn.pid?.let { append(" (PID $it)") }
                }
                Text(
                    text = procText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // State chip
        val stateColor = when (conn.state.uppercase()) {
            "ESTABLISHED" -> Color(0xFF2E7D32)
            "LISTEN" -> Color(0xFF1565C0)
            "TIME_WAIT" -> Color(0xFFE65100)
            "CLOSE_WAIT" -> Color(0xFFF9A825)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val stateTextColor = when (conn.state.uppercase()) {
            "ESTABLISHED", "LISTEN", "TIME_WAIT" -> Color.White
            "CLOSE_WAIT" -> Color.Black
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = stateColor
        ) {
            Text(
                text = conn.state,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = stateTextColor
            )
        }
    }
}
