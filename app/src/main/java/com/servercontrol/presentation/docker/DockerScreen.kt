package com.servercontrol.presentation.docker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.DockerAction
import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.model.DockerImage
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockerScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: DockerViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val containers by viewModel.containers.collectAsState()
    val images by viewModel.images.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val containerLogs by viewModel.containerLogs.collectAsState()
    val selectedContainer by viewModel.selectedContainer.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionResult) {
        when (val r = actionResult) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(r.data.ifBlank { "Action completed" })
                viewModel.clearActionResult()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar("Error: ${r.message}")
                viewModel.clearActionResult()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Docker") },
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
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { viewModel.selectedTab.value = 0 }) {
                    Text("Containers", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { viewModel.selectedTab.value = 1 }) {
                    Text("Images", modifier = Modifier.padding(16.dp))
                }
            }

            when (selectedTab) {
                0 -> ContainersTab(
                    containers = containers,
                    onContainerClick = { viewModel.selectContainer(it) },
                    onAction = { id, action -> viewModel.performAction(id, action) }
                )
                1 -> ImagesTab(images = images)
            }
        }
    }

    selectedContainer?.let { container ->
        ContainerDetailBottomSheet(
            container = container,
            logs = containerLogs,
            onDismiss = { viewModel.selectContainer(null) },
            onLoadLogs = { viewModel.loadLogs(container.id) },
            onAction = { action -> viewModel.performAction(container.id, action) }
        )
    }
}

@Composable
private fun ContainersTab(
    containers: Resource<List<DockerContainer>>,
    onContainerClick: (DockerContainer) -> Unit,
    onAction: (String, DockerAction) -> Unit
) {
    when (containers) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${containers.message}", color = MaterialTheme.colorScheme.error)
        }
        is Resource.Success -> {
            if (containers.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No containers found")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(containers.data) { container ->
                        ContainerCard(
                            container = container,
                            onClick = { onContainerClick(container) },
                            onAction = { action -> onAction(container.id, action) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContainerCard(
    container: DockerContainer,
    onClick: () -> Unit,
    onAction: (DockerAction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(containerStatusColor(container.state), CircleShape)
                )
                Text(
                    text = container.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DockerAction.values().filter { it != DockerAction.REMOVE }.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { onAction(action); showMenu = false }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                            onClick = { onAction(DockerAction.REMOVE); showMenu = false }
                        )
                    }
                }
            }
            Text(
                text = container.image,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (container.ports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    container.ports.take(3).forEach { port ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${port.hostPort}→${port.containerPort}", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            if (container.state == "running") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "CPU: ${"%.1f".format(container.cpuPercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (container.memLimitBytes > 0) {
                        val memPct = container.memUsedBytes.toDouble() / container.memLimitBytes * 100
                        Text(
                            "MEM: ${"%.1f".format(memPct)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagesTab(images: Resource<List<DockerImage>>) {
    when (images) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${images.message}", color = MaterialTheme.colorScheme.error)
        }
        is Resource.Success -> {
            if (images.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No images found")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images.data) { image ->
                        ImageRow(image = image)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageRow(image: DockerImage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = image.tags.firstOrNull() ?: "<none>",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = image.id.take(12),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (image.sizeBytes > 0) {
                AssistChip(
                    onClick = {},
                    label = { Text(formatBytes(image.sizeBytes), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerDetailBottomSheet(
    container: DockerContainer,
    logs: Resource<List<String>>?,
    onDismiss: () -> Unit,
    onLoadLogs: () -> Unit,
    onAction: (DockerAction) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(container.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(container.image, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(container.state) },
                    leadingIcon = {
                        Box(modifier = Modifier.size(8.dp).background(containerStatusColor(container.state), CircleShape))
                    }
                )
            }

            if (container.state == "running") {
                HorizontalDivider()
                Text("Resource Usage", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CPU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${"%.2f".format(container.cpuPercent)}%", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (container.memLimitBytes > 0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Memory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${formatBytes(container.memUsedBytes)} / ${formatBytes(container.memLimitBytes)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Net RX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatBytes(container.networkRxBytes), style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Net TX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatBytes(container.networkTxBytes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (container.ports.isNotEmpty()) {
                HorizontalDivider()
                Text("Ports", style = MaterialTheme.typography.labelLarge)
                container.ports.forEach { port ->
                    Text(
                        "${port.hostPort}:${port.containerPort}/${port.protocol}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider()
            Text("Actions", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (container.state != "running") {
                    OutlinedButton(onClick = { onAction(DockerAction.START) }, modifier = Modifier.weight(1f)) {
                        Text("Start")
                    }
                } else {
                    OutlinedButton(onClick = { onAction(DockerAction.STOP) }, modifier = Modifier.weight(1f)) {
                        Text("Stop")
                    }
                    OutlinedButton(onClick = { onAction(DockerAction.RESTART) }, modifier = Modifier.weight(1f)) {
                        Text("Restart")
                    }
                }
            }

            HorizontalDivider()
            Button(onClick = onLoadLogs, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View Logs")
            }

            when (val l = logs) {
                is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                is Resource.Success -> {
                    Surface(
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            l.data.takeLast(50).forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> Text("Failed to load logs: ${l.message}", color = MaterialTheme.colorScheme.error)
                null -> {}
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun containerStatusColor(state: String): Color = when (state.lowercase()) {
    "running" -> Color(0xFF4CAF50)
    "exited", "dead" -> Color(0xFF9E9E9E)
    "paused" -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
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
