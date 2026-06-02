package com.servercontrol.presentation.docker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.DockerAction
import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.model.DockerImage
import com.servercontrol.presentation.components.ServerStatus
import com.servercontrol.presentation.components.StatusChip
import com.servercontrol.presentation.theme.*
import com.servercontrol.util.FormatUtils
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
    val selectedContainer by viewModel.selectedContainer.collectAsState()
    val containerLogs by viewModel.containerLogs.collectAsState()
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Containers",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${containers.message}", color = MaterialTheme.colorScheme.error)
        }
        is Resource.Success -> {
            val list = containers.data
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No containers found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary row
                    item {
                        val running = list.count { it.state == "running" }
                        val issues = list.count { it.state in listOf("exited", "dead", "error") }
                        SummaryRow(running = running, total = list.size, issues = issues)
                    }
                    // Container cards
                    items(list) { container ->
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
private fun SummaryRow(running: Int, total: Int, issues: Int) {
    val sc = LocalStatusColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryStatCard("Running", running.toString(), sc.online, Modifier.weight(1f))
        SummaryStatCard("Total", total.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        SummaryStatCard("Issues", issues.toString(), sc.warn, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = WellShape,
        color = SC3
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContainerCard(
    container: DockerContainer,
    onClick: () -> Unit,
    onAction: (DockerAction) -> Unit
) {
    val isRunning = container.state == "running"
    val sc = LocalStatusColors.current
    val statusChipStatus = when {
        isRunning -> ServerStatus.ONLINE
        container.state == "paused" -> ServerStatus.WARN
        else -> ServerStatus.DOWN
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        shape = CardShape,
        color = SC2,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(SC3),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ViewInAr,
                        contentDescription = null,
                        tint = if (isRunning) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        container.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        container.image,
                        fontSize = 12.sp,
                        fontFamily = MonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(status = statusChipStatus)
            }

            // Footer
            HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isRunning) {
                    // Inline stats + action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Bolt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Text(
                            "${"%.1f".format(container.cpuPercent)}%",
                            fontSize = 12.sp,
                            fontFamily = MonoFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (container.memLimitBytes > 0) {
                            Icon(Icons.Filled.Memory, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Text(
                                formatBytes(container.memUsedBytes),
                                fontSize = 12.sp,
                                fontFamily = MonoFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionIconButton(onClick = { onAction(DockerAction.STOP) }, icon = Icons.Filled.Stop)
                        ActionIconButton(onClick = { onAction(DockerAction.RESTART) }, icon = Icons.Filled.RestartAlt)
                    }
                } else {
                    Text(
                        text = container.state,
                        fontFamily = MonoFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onAction(DockerAction.START) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionIconButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SC3)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ImagesTab(images: Resource<List<DockerImage>>) {
    when (images) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${images.message}", color = MaterialTheme.colorScheme.error)
        }
        is Resource.Success -> {
            if (images.data.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No images found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images.data) { image ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            color = SC2,
                            border = BorderStroke(1.dp, OutlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = image.tags.firstOrNull() ?: "<none>",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = image.id.take(12),
                                    fontFamily = MonoFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (image.sizeBytes > 0) {
                                    Text(
                                        text = formatBytes(image.sizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
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
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Icon(Icons.Filled.Description, null)
                Spacer(Modifier.width(8.dp))
                Text("View Logs")
            }
            when (val l = logs) {
                is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                is Resource.Success -> {
                    Surface(
                        color = SC1,
                        shape = WellShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            l.data.takeLast(50).forEach { line ->
                                Text(
                                    line,
                                    fontSize = 11.sp,
                                    fontFamily = MonoFamily,
                                    color = MaterialTheme.colorScheme.onSurface
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

private fun formatBytes(bytes: Long): String = FormatUtils.formatBytes(bytes)
