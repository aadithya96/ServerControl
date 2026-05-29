package com.servercontrol.presentation.services

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.ServiceAction
import com.servercontrol.domain.model.SystemService
import com.servercontrol.presentation.dashboard.ShimmerCard
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceManagerScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ServiceManagerViewModel = hiltViewModel()
) {
    val services by viewModel.services.collectAsState()
    val stateFilter by viewModel.stateFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val serviceLogs by viewModel.serviceLogs.collectAsState()

    val serviceList = (services as? Resource.Success)?.data ?: emptyList()
    val failedCount = serviceList.count { it.activeState == "failed" }

    if (actionResult is Resource.Error) {
        LaunchedEffect(actionResult) {
            // reset after showing
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Services")
                        if (serviceList.isNotEmpty()) {
                            Text(
                                "${serviceList.size} units",
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
            // State filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stateFilters = listOf("all" to "All", "active" to "Active", "failed" to "Failed", "inactive" to "Inactive")
                items(stateFilters) { (value, label) ->
                    FilterChip(
                        selected = stateFilter == value,
                        onClick = { viewModel.setStateFilter(value) },
                        label = {
                            if (value == "failed" && failedCount > 0) {
                                Text("Failed ($failedCount)", color = if (stateFilter == value) Color.White else MaterialTheme.colorScheme.error)
                            } else {
                                Text(label)
                            }
                        },
                        colors = if (value == "failed" && failedCount > 0 && stateFilter != value) {
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        } else FilterChipDefaults.filterChipColors()
                    )
                }
            }

            // Type filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val typeFilters = listOf("all" to "All", "service" to "Service", "timer" to "Timer", "socket" to "Socket")
                items(typeFilters) { (value, label) ->
                    FilterChip(
                        selected = typeFilter == value,
                        onClick = { viewModel.setTypeFilter(value) },
                        label = { Text(label) }
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search services…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            when (val s = services) {
                is Resource.Loading -> {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(6) { ShimmerCard() }
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
                    val all = s.data
                    val failed = all.filter { it.activeState == "failed" }
                    val others = all.filter { it.activeState != "failed" }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (failed.isNotEmpty()) {
                            item {
                                Text(
                                    "Failed",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            items(failed, key = { it.name }) { service ->
                                ServiceRow(
                                    service = service,
                                    isFailed = true,
                                    onClick = { viewModel.selectService(service) },
                                    onAction = { action -> viewModel.performAction(service.name, action) }
                                )
                            }
                        }
                        if (others.isNotEmpty()) {
                            item {
                                Text(
                                    "Services",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(others, key = { it.name }) { service ->
                                ServiceRow(
                                    service = service,
                                    isFailed = false,
                                    onClick = { viewModel.selectService(service) },
                                    onAction = { action -> viewModel.performAction(service.name, action) }
                                )
                            }
                        }
                        if (all.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No services found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Service detail bottom sheet
        selectedService?.let { service ->
            ServiceDetailBottomSheet(
                service = service,
                logs = serviceLogs,
                onDismiss = { viewModel.selectService(null) },
                onAction = { action -> viewModel.performAction(service.name, action) },
                onLoadLogs = { viewModel.loadLogs(service.name) }
            )
        }
    }
}

@Composable
private fun ServiceRow(
    service: SystemService,
    isFailed: Boolean,
    onClick: () -> Unit,
    onAction: (ServiceAction) -> Unit
) {
    val borderColor = if (isFailed) MaterialTheme.colorScheme.error else Color.Transparent
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(serviceStateColor(service))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    service.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (service.description.isNotBlank()) {
                    Text(
                        service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    EnabledBadge(service.enabled, service.unitFilePath.isNotBlank())
                    Text(
                        service.subState,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    ServiceAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                menuExpanded = false
                                onAction(action)
                            }
                        )
                    }
                }
            }
        }
        if (isFailed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Composable
private fun EnabledBadge(enabled: Boolean, hasUnitFile: Boolean) {
    val label = when {
        enabled -> "enabled"
        !hasUnitFile -> "static"
        else -> "disabled"
    }
    val colors = when (label) {
        "enabled" -> FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF2E7D32),
            labelColor = Color.White
        )
        "static" -> FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFF1565C0).copy(alpha = 0.15f),
            labelColor = Color(0xFF1565C0)
        )
        else -> FilterChipDefaults.filterChipColors()
    }
    FilterChip(
        selected = false,
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = colors,
        modifier = Modifier.height(22.dp)
    )
}

@Composable
private fun serviceStateColor(service: SystemService): Color = when {
    service.activeState == "failed" -> MaterialTheme.colorScheme.error
    service.subState == "running" && service.activeState == "active" -> Color(0xFF4CAF50)
    service.activeState == "activating" || service.activeState == "deactivating" -> Color(0xFFFFC107)
    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDetailBottomSheet(
    service: SystemService,
    logs: Resource<List<String>>?,
    onDismiss: () -> Unit,
    onAction: (ServiceAction) -> Unit,
    onLoadLogs: () -> Unit
) {
    val listState = rememberLazyListState()
    val logLines = (logs as? Resource.Success)?.data ?: emptyList()
    val logsLoading = logs is Resource.Loading

    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(service.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (service.description.isNotBlank()) {
                Text(
                    service.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Details grid
            val details = listOf(
                "Load State" to service.loadState,
                "Active State" to service.activeState,
                "Sub State" to service.subState,
                "Type" to service.type
            )
            details.forEach { (k, v) ->
                if (v.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (service.unitFilePath.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Unit File", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(service.unitFilePath, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }

            if (service.execStart.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("ExecStart", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        service.execStart,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAction(ServiceAction.START) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f)
                ) { Text("Start") }
                Button(
                    onClick = { onAction(ServiceAction.STOP) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
                Button(
                    onClick = { onAction(ServiceAction.RESTART) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.weight(1f)
                ) { Text("Restart") }
                Button(
                    onClick = { onAction(ServiceAction.RELOAD) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    modifier = Modifier.weight(1f)
                ) { Text("Reload") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction(ServiceAction.ENABLE) },
                    modifier = Modifier.weight(1f)
                ) { Text("Enable") }
                OutlinedButton(
                    onClick = { onAction(ServiceAction.DISABLE) },
                    modifier = Modifier.weight(1f)
                ) { Text("Disable") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLoadLogs,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Logs")
            }

            if (logsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }

            if (logLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logLines) { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }
        }
    }
}
