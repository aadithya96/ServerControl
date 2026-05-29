package com.servercontrol.presentation.logs

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.LogEntry
import com.servercontrol.domain.model.LogLevel
import com.servercontrol.presentation.dashboard.ShimmerCard
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
    val selectedUnit by viewModel.selectedUnit.collectAsState()
    val lineCount by viewModel.lineCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val autoRefresh by viewModel.autoRefresh.collectAsState()
    val autoScroll by viewModel.autoScroll.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val logList = (logs as? Resource.Success)?.data ?: emptyList()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logList.size, autoScroll) {
        if (autoScroll && logList.isNotEmpty()) {
            listState.animateScrollToItem(logList.size - 1)
        }
    }

    // Unit / custom path state
    var unitInput by remember { mutableStateOf(selectedUnit ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Logs")
                        Text(
                            "$lineCount lines${if (autoRefresh) " • Live" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleAutoScroll) {
                        Icon(
                            if (autoScroll) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardDoubleArrowDown,
                            contentDescription = "Auto-scroll",
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val text = viewModel.exportLogs()
                        if (text.isNotBlank()) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
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
            // Source selector
            LazyRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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

            // Unit / custom path input
            if (selectedSource == "journal" || selectedSource == "custom") {
                val label = if (selectedSource == "journal") "Unit name (e.g. nginx, sshd)" else "File path (e.g. /var/log/app.log)"
                OutlinedTextField(
                    value = unitInput,
                    onValueChange = { unitInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text(label) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.setSource(selectedSource, if (unitInput.isBlank()) null else unitInput)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Apply")
                        }
                    }
                )
            }

            // Line count chips
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 200, 500, 1000).forEach { n ->
                    FilterChip(
                        selected = lineCount == n,
                        onClick = { viewModel.setLineCount(n) },
                        label = { Text("$n") }
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
                placeholder = { Text("Search logs…") },
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

            // Auto refresh indicator
            if (autoRefresh) {
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.4f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "pulse_alpha"
                )
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = pulse), RoundedCornerShape(50))
                    )
                    Text("Live", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                }
            }

            // Toggle auto-refresh button
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::toggleAutoRefresh,
                    colors = if (autoRefresh) ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(
                        if (autoRefresh) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (autoRefresh) "Stop Live" else "Start Live")
                }
            }

            // Log content
            when (val l = logs) {
                is Resource.Loading -> {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(8) { ShimmerCard() }
                    }
                }
                is Resource.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(l.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = viewModel::refresh) { Text("Retry") }
                        }
                    }
                }
                is Resource.Success -> {
                    if (l.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No log entries found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(l.data) { _, entry ->
                                LogEntryRow(entry = entry, searchQuery = searchQuery)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry, searchQuery: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (entry.timestamp.isNotBlank()) {
            Text(
                entry.timestamp.take(19).replace("T", " "),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(118.dp)
            )
        }
        LevelBadge(entry.level)
        val annotated = remember(entry.message, searchQuery) {
            LogParser.highlight(entry, searchQuery)
        }
        Text(
            annotated,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Clip,
            softWrap = true
        )
    }
}

@Composable
private fun LevelBadge(level: LogLevel) {
    val (label, color) = when (level) {
        LogLevel.ERROR -> "ERR" to Color(0xFFF44336)
        LogLevel.WARN -> "WRN" to Color(0xFFFFC107)
        LogLevel.INFO -> "INF" to Color(0xFF2196F3)
        LogLevel.DEBUG -> "DBG" to Color(0xFF9E9E9E)
        LogLevel.UNKNOWN -> "???" to Color(0xFF757575)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.width(32.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}
