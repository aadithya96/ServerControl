package com.servercontrol.presentation.servers

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.presentation.components.Meter
import com.servercontrol.presentation.components.ServerStatus
import com.servercontrol.presentation.components.StatusChip
import com.servercontrol.presentation.theme.*
import com.servercontrol.util.ProfileExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    onServerClick: (Long) -> Unit,
    onAddServer: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenTerminal: (Long) -> Unit = {},
    onInstallAgent: (Long) -> Unit = {},
    onEditServer: (Long) -> Unit = {},
    onOverview: () -> Unit = {},
    onShareQr: (Long) -> Unit = {},
    onScanQr: () -> Unit = {},
    onExportProfiles: () -> Unit = {},
    onImportProfiles: () -> Unit = {},
    viewModel: ServerListViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsState()
    var serverToDelete by remember { mutableStateOf<ServerProfile?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val onlineCount = servers.count { it.isOnline }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val json = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            } catch (e: Exception) { "" }
            if (json.isNotBlank()) {
                ProfileExporter.importFromJson(json).forEach { profile ->
                    viewModel.importServer(profile)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddServer,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add host", style = MaterialTheme.typography.titleSmall)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Servers",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$onlineCount of ${servers.size} online · ${servers.size} hosts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Search button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SC3)
                                .clickable { /* TODO: search */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Overflow
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SC3)
                                    .clickable { showOverflowMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Overview") },
                                    onClick = { showOverflowMenu = false; onOverview() },
                                    leadingIcon = { Icon(Icons.Filled.GridView, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Profiles") },
                                    onClick = {
                                        showOverflowMenu = false
                                        ProfileExporter.shareProfiles(context, servers)
                                    },
                                    leadingIcon = { Icon(Icons.Filled.IosShare, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Profiles") },
                                    onClick = {
                                        showOverflowMenu = false
                                        importLauncher.launch(arrayOf("application/json", "*/*"))
                                    },
                                    leadingIcon = { Icon(Icons.Filled.FileUpload, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = { showOverflowMenu = false; onSettingsClick() },
                                    leadingIcon = { Icon(Icons.Filled.Settings, null) }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (servers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Dns,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                "No servers yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tap \"Add host\" to connect your first server",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            items(servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    onClick = {
                        if (server.isOnline) {
                            viewModel.selectServer(server.id)
                            onServerClick(server.id)
                        }
                    },
                    onDelete = { serverToDelete = server },
                    onOpenTerminal = { onOpenTerminal(server.id) },
                    onInstallAgent = { onInstallAgent(server.id) },
                    onEdit = { onEditServer(server.id) },
                    onShareQr = { onShareQr(server.id) }
                )
            }
        }
    }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text("Delete Server") },
            text = { Text("Remove \"${server.name}\" from the list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteServer(server)
                    serverToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerProfile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenTerminal: () -> Unit = {},
    onInstallAgent: () -> Unit = {},
    onEdit: () -> Unit = {},
    onShareQr: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val status = if (server.isOnline) ServerStatus.ONLINE else ServerStatus.DOWN

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (server.isOnline) 1f else 0.72f)
            .clip(CardShape)
            .then(if (server.isOnline) Modifier.clickable(onClick = onClick) else Modifier),
        color = SC2,
        shape = CardShape,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Column {
            // Top row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon tile with status dot
                Box(modifier = Modifier.size(46.dp)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SC4),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Status dot badge
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    if (server.isOnline) StatusOnlineColor else StatusDownColor
                                )
                        )
                    }
                }

                // Name + host
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${server.host}:${server.agentPort}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = MonoFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status chip + overflow menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusChip(status = status)
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open Terminal") },
                                onClick = { showMenu = false; onOpenTerminal() },
                                leadingIcon = { Icon(Icons.Filled.Terminal, null) }
                            )
                            if (server.authType == AuthType.SSH_PASSWORD || server.authType == AuthType.SSH_KEY) {
                                DropdownMenuItem(
                                    text = { Text("Install Agent") },
                                    onClick = { showMenu = false; onInstallAgent() },
                                    leadingIcon = { Icon(Icons.Filled.Download, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Share as QR") },
                                onClick = { showMenu = false; onShareQr() },
                                leadingIcon = { Icon(Icons.Filled.QrCode, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }

            // Metric strip or offline row
            HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
            if (server.isOnline) {
                MetricStrip()
            } else {
                OfflineRow()
            }
        }
    }
}

@Composable
private fun MetricStrip() {
    // Metrics are fetched per-server in real usage; show layout with 0 values here
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
    ) {
        MetricColumn(label = "CPU", value = "—", fillFraction = 0f, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.width(1.dp).height(48.dp).background(OutlineVariant))
        MetricColumn(label = "MEM", value = "—", fillFraction = 0f, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.width(1.dp).height(48.dp).background(OutlineVariant))
        MetricColumn(label = "DISK", value = "—", fillFraction = 0f, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    fillFraction: Float,
    modifier: Modifier = Modifier
) {
    val fillColor = if (fillFraction > 0.8f) StatusWarnColor else Primary
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp
        )
        Meter(
            value = fillFraction,
            fillColor = fillColor,
            modifier = Modifier.fillMaxWidth(),
            barHeight = 5.dp
        )
    }
}

@Composable
private fun OfflineRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = StatusDownColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Connection refused · check host reachability",
            style = MaterialTheme.typography.bodySmall,
            color = StatusDownColor
        )
    }
}
