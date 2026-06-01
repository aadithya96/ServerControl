package com.servercontrol.presentation.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private val TerminalBg = Color(0xFF0D1117)
private val TerminalTextColor = Color(0xFFE6EDF3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentInstallerScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    onAgentConfigured: () -> Unit,
    viewModel: AgentInstallerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val outputLines by viewModel.outputLines.collectAsState()
    val serverName by viewModel.serverName.collectAsState()
    val serverHost by viewModel.serverHost.collectAsState()

    var showUninstallDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Installer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = serverName.ifBlank { "Server" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = serverHost,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Status chip
                    val (chipText, chipColor) = when (state) {
                        is AgentInstallerViewModel.InstallerState.CheckingPrereqs ->
                            "Checking..." to MaterialTheme.colorScheme.onSurfaceVariant
                        is AgentInstallerViewModel.InstallerState.Ready -> {
                            val ready = state as AgentInstallerViewModel.InstallerState.Ready
                            if (ready.hasAgent) "Agent Running" to Color(0xFF4CAF50)
                            else "Not Installed" to Color(0xFFF44336)
                        }
                        is AgentInstallerViewModel.InstallerState.Success ->
                            "Installed" to Color(0xFF4CAF50)
                        is AgentInstallerViewModel.InstallerState.Error ->
                            "Error" to Color(0xFFF44336)
                        else -> "Checking..." to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(chipText, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            labelColor = chipColor
                        )
                    )
                }
            }

            // State-specific content
            when (val currentState = state) {
                is AgentInstallerViewModel.InstallerState.Idle,
                is AgentInstallerViewModel.InstallerState.CheckingPrereqs -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Checking server prerequisites...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is AgentInstallerViewModel.InstallerState.Ready -> {
                    if (currentState.hasAgent) {
                        // Already installed
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        "Agent is already installed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (currentState.agentVersion != null) {
                                        Text(
                                            "Version: ${currentState.agentVersion}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = viewModel::install,
                                modifier = Modifier.weight(1f)
                            ) { Text("Reinstall") }
                            TextButton(
                                onClick = { showUninstallDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Uninstall") }
                        }
                    } else {
                        // Not installed — show info and install button
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "What the installer does:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "• Downloads and installs the ServerControl agent binary\n" +
                                    "• Creates a systemd service for auto-start\n" +
                                    "• Generates a secure authentication token\n" +
                                    "• Starts the agent on port 9876",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Requirements:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                RequirementRow("curl", currentState.hasCurl)
                                RequirementRow("systemd", currentState.hasSystemd)
                                RequirementRow("sudo access", true) // assumed if we SSH'd in
                            }
                        }

                        Button(
                            onClick = viewModel::install,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentState.hasCurl
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Install Agent")
                        }
                    }
                }

                is AgentInstallerViewModel.InstallerState.Installing,
                is AgentInstallerViewModel.InstallerState.StreamingOutput -> {
                    val lines = when (currentState) {
                        is AgentInstallerViewModel.InstallerState.StreamingOutput -> currentState.lines
                        else -> outputLines
                    }
                    val isComplete = currentState is AgentInstallerViewModel.InstallerState.StreamingOutput &&
                        currentState.isComplete

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isComplete) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            if (isComplete) "Installation complete" else "Installing…",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    InstallerOutputCard(lines = lines)
                }

                is AgentInstallerViewModel.InstallerState.Success -> {
                    // Success state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                "Agent Installed Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )

                            HorizontalDivider()

                            Text(
                                "Authentication Token",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(TerminalBg, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentState.token,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TerminalTextColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState())
                                )
                                IconButton(
                                    onClick = { viewModel.copyToken(currentState.token) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy token",
                                        modifier = Modifier.size(16.dp),
                                        tint = TerminalTextColor
                                    )
                                }
                            }

                            Text(
                                "Listening on port ${currentState.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                "Save this token — it won't be shown again",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.applyToken(currentState.token, currentState.port)
                            onAgentConfigured()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Apply to Server Profile")
                    }
                }

                is AgentInstallerViewModel.InstallerState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                currentState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (currentState.logs.isNotEmpty()) {
                        InstallerOutputCard(lines = currentState.logs)
                    }
                    Button(
                        onClick = viewModel::checkPrereqs,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Try Again") }
                }
            }
        }
    }

    if (showUninstallDialog) {
        AlertDialog(
            onDismissRequest = { showUninstallDialog = false },
            title = { Text("Uninstall Agent") },
            text = { Text("This will stop and remove the agent from the server. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUninstallDialog = false
                        viewModel.uninstall()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Uninstall") }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RequirementRow(name: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (met) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (met) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(16.dp)
        )
        Text(name, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InstallerOutputCard(lines: List<String>) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 400.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalBg)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(lines) { line ->
                val lineColor = when {
                    line.contains("error", ignoreCase = true) ||
                    line.contains("failed", ignoreCase = true) -> Color(0xFFFF5555)
                    line.contains("✓") || line.contains("ok", ignoreCase = true) ||
                    line.contains("done", ignoreCase = true) ||
                    line.contains("success", ignoreCase = true) -> Color(0xFF55FF55)
                    else -> TerminalTextColor
                }
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = lineColor,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
