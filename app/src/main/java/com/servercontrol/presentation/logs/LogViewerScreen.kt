package com.servercontrol.presentation.logs

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.LogEntry
import com.servercontrol.domain.model.LogLevel
import com.servercontrol.presentation.theme.*
import com.servercontrol.util.LogParser
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: LogViewerViewModel = hiltViewModel()
) {
    val logs by viewModel.filteredLogs.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val lineCount by viewModel.lineCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val autoRefresh by viewModel.autoRefresh.collectAsState()
    val autoScroll by viewModel.autoScroll.collectAsState()
    val selectedUnit by viewModel.selectedUnit.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val logList = (logs as? Resource.Success)?.data ?: emptyList()

    var levelFilter by remember { mutableStateOf<LogLevel?>(null) }
    val filteredByLevel = if (levelFilter == null) logList
                          else logList.filter { it.level == levelFilter }

    LaunchedEffect(filteredByLevel.size, autoScroll) {
        if (autoScroll && filteredByLevel.isNotEmpty()) {
            listState.animateScrollToItem(filteredByLevel.size - 1)
        }
    }

    var unitInput by remember { mutableStateOf(selectedUnit ?: "") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Logs",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${lineCount} lines${if (autoRefresh) " · Live" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = viewModel.exportLogs()
                        if (text.isNotBlank()) {
                            context.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }, "Export Logs"
                            ))
                        }
                    }) {
                        Icon(Icons.Filled.Share, "Export")
                    }
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
            // Level filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val levels = listOf(null to "ALL") +
                    listOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.DEBUG)
                        .map { it to it.name }

                items(levels.size) { idx ->
                    val (level, label) = levels[idx]
                    val selected = levelFilter == level
                    val chipTextColor = when {
                        selected -> MaterialTheme.colorScheme.onSecondaryContainer
                        level == LogLevel.WARN -> StatusWarnColor
                        level == LogLevel.ERROR -> StatusDownColor
                        level == LogLevel.DEBUG -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        onClick = { levelFilter = level },
                        shape = ChipShape,
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.background,
                        border = if (selected) null else BorderStroke(1.dp, OutlineVariant)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = chipTextColor
                        )
                    }
                }
            }

            // Source selector chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.availableSources) { source ->
                    FilterChip(
                        selected = selectedSource == source.id,
                        onClick = {
                            if (source.id != "journal" && source.id != "custom") {
                                viewModel.setSource(source.id)
                                unitInput = ""
                            } else {
                                viewModel.setSource(source.id, if (unitInput.isBlank()) null else unitInput)
                            }
                        },
                        label = { Text(source.displayName) }
                    )
                }
            }

            // Unit input for journal/custom
            if (selectedSource == "journal" || selectedSource == "custom") {
                val label = if (selectedSource == "journal") "Unit name (e.g. nginx, sshd)"
                            else "File path (e.g. /var/log/app.log)"
                OutlinedTextField(
                    value = unitInput,
                    onValueChange = { unitInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(label) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.setSource(selectedSource, if (unitInput.isBlank()) null else unitInput)
                        }) {
                            Icon(Icons.Filled.Search, "Apply")
                        }
                    }
                )
            }

            // Log console surface
            when (val l = logs) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is Resource.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(l.message, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = viewModel::refresh) { Text("Retry") }
                        }
                    }
                }
                is Resource.Success -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        // Console card
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, OutlineVariant, WellShape),
                            shape = WellShape,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (filteredByLevel.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No log entries",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    itemsIndexed(filteredByLevel) { _, entry ->
                                        LogLine(entry = entry, searchQuery = searchQuery)
                                        HorizontalDivider(color = SC2, thickness = 1.dp)
                                    }
                                }
                            }
                        }

                        // Live footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (autoRefresh) {
                                LiveIndicator()
                            } else {
                                TextButton(onClick = viewModel::toggleAutoRefresh) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start Live", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (autoRefresh) {
                                TextButton(onClick = viewModel::toggleAutoRefresh) {
                                    Icon(Icons.Filled.Stop, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Stop", fontSize = 12.sp)
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
private fun LiveIndicator() {
    val sc = LocalStatusColors.current
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(sc.online.copy(alpha = pulse))
        )
        Text(
            "streaming live · tail -f",
            fontFamily = MonoFamily,
            fontSize = 11.sp,
            color = sc.online
        )
    }
}

@Composable
private fun LogLine(entry: LogEntry, searchQuery: String) {
    val sc = LocalStatusColors.current
    val levelColor = when (entry.level) {
        LogLevel.ERROR -> sc.down
        LogLevel.WARN  -> sc.warn
        LogLevel.INFO  -> sc.info
        LogLevel.DEBUG -> MaterialTheme.colorScheme.primary
        else           -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val levelLabel = when (entry.level) {
        LogLevel.ERROR -> "ERR"
        LogLevel.WARN  -> "WRN"
        LogLevel.INFO  -> "INF"
        LogLevel.DEBUG -> "DBG"
        else           -> "???"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (entry.timestamp.isNotBlank()) {
            Text(
                entry.timestamp.take(19).replace("T", " "),
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(110.dp)
            )
        }
        // Level tag
        Text(
            levelLabel,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = levelColor,
            modifier = Modifier.width(28.dp)
        )
        // Message
        val annotated = remember(entry.message, searchQuery) {
            LogParser.highlight(entry, searchQuery)
        }
        Text(
            annotated,
            fontFamily = MonoFamily,
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Clip,
            softWrap = true
        )
    }
}
