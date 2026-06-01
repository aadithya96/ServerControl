package com.servercontrol.presentation.processes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.ProcessSortOrder
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.presentation.theme.*

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
    val sortBy by viewModel.sortBy.collectAsState()

    LaunchedEffect(state.killResult) {
        state.killResult?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Processes",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (state.processes.isNotEmpty()) {
                            Text(
                                "${state.processes.size} running",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sort chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sortOptions = listOf(
                    ProcessSortOrder.CPU to "CPU",
                    ProcessSortOrder.MEMORY to "Memory",
                    ProcessSortOrder.PID to "PID"
                )
                sortOptions.forEach { (order, label) ->
                    val selected = sortBy == order
                    SortChip(
                        label = label,
                        selected = selected,
                        onClick = { viewModel.setSortOrder(order) }
                    )
                }
                Spacer(Modifier.weight(1f))
                // Filter button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(1.dp, OutlineVariant, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.FilterList,
                        contentDescription = "Filter",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                state.isLoading -> LoadingSpinner(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                state.error != null -> ErrorState(
                    message = state.error!!,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                )
                else -> {
                    // List card
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .clip(CardShape)
                            .border(1.dp, OutlineVariant, CardShape)
                            .background(SC2),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        items(state.processes, key = { it.pid }) { process ->
                            ProcessRow(
                                process = process,
                                onLongPress = { processForBottomSheet = process }
                            )
                            HorizontalDivider(
                                color = OutlineVariant,
                                thickness = 1.dp
                            )
                        }
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

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ChipShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background,
        border = if (selected) null else BorderStroke(1.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProcessRow(process: Process, onLongPress: () -> Unit) {
    val sc = LocalStatusColors.current
    val stateColor = when (process.status.uppercase().firstOrNull()) {
        'R' -> sc.online
        'S' -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val cpuWarn = process.cpuPercent > 10.0
    val cpuColor = if (cpuWarn) sc.warn else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // State tile
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(SC3),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = process.status.uppercase().take(1).ifEmpty { "?" },
                fontSize = 13.sp,
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Bold,
                color = stateColor
            )
        }

        // Name + PID · user
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = process.name,
                fontFamily = MonoFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "PID ${process.pid} · ${process.user}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CPU% + MEM% right-aligned
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            StatPair(
                label = "CPU",
                value = "${"%.1f".format(process.cpuPercent)}%",
                valueColor = cpuColor
            )
            StatPair(
                label = "MEM",
                value = "${"%.1f".format(process.memPercent)}%",
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatPair(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontFamily = MonoFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.4.sp
        )
        Text(
            value,
            fontFamily = MonoFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProcessBottomSheet(
    process: Process,
    onDismiss: () -> Unit,
    onKill: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                process.name,
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
                    process.command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = MonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
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
