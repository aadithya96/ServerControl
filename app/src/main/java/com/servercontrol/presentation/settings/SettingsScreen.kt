package com.servercontrol.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.BuildConfig
import com.servercontrol.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onQrTransfer: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showRefreshDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile card
            item {
                ProfileCard(onQrClick = onQrTransfer)
            }

            // Account section
            item {
                SettingsSection(
                    label = "ACCOUNT",
                    items = listOf(
                        SettingRowData(
                            icon = Icons.Filled.Notifications,
                            title = "Alert Notifications",
                            trailing = {
                                Switch(
                                    checked = state.backgroundMonitoringEnabled,
                                    onCheckedChange = viewModel::setBackgroundMonitoringEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedTrackColor = SC4
                                    )
                                )
                            }
                        ),
                        SettingRowData(
                            icon = Icons.Filled.QrCode2,
                            title = "QR Transfer",
                            trailingText = "Tap to share",
                            onClick = onQrTransfer
                        )
                    )
                )
            }

            // Monitoring section
            item {
                SettingsSection(
                    label = "MONITORING",
                    items = listOf(
                        SettingRowData(
                            icon = Icons.Filled.Timer,
                            title = "Refresh Interval",
                            trailingText = "${state.refreshInterval}s",
                            onClick = { showRefreshDialog = true }
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Sync,
                            title = "Background Monitoring",
                            trailing = {
                                Switch(
                                    checked = state.backgroundMonitoringEnabled,
                                    onCheckedChange = viewModel::setBackgroundMonitoringEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedTrackColor = SC4
                                    )
                                )
                            }
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Memory,
                            title = "CPU Alert Threshold",
                            trailingText = "${state.cpuAlertThreshold}%",
                            onClick = {}
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Storage,
                            title = "Disk Alert Threshold",
                            trailingText = "${state.diskAlertThreshold}%",
                            onClick = {}
                        )
                    )
                )
            }

            // Security section
            item {
                SettingsSection(
                    label = "SECURITY",
                    items = listOf(
                        SettingRowData(
                            icon = Icons.Filled.Fingerprint,
                            title = "Biometric Lock",
                            trailing = {
                                Switch(
                                    checked = false,
                                    onCheckedChange = {},
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedTrackColor = SC4
                                    )
                                )
                            }
                        ),
                        SettingRowData(
                            icon = Icons.Filled.VpnLock,
                            title = "VPN Detection",
                            trailing = {
                                Switch(
                                    checked = false,
                                    onCheckedChange = {},
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedTrackColor = SC4
                                    )
                                )
                            }
                        )
                    )
                )
            }

            // App section
            item {
                SettingsSection(
                    label = "APP",
                    items = listOf(
                        SettingRowData(
                            icon = Icons.Filled.Palette,
                            title = "Dark Theme",
                            trailing = {
                                Switch(
                                    checked = state.isDarkTheme,
                                    onCheckedChange = viewModel::setDarkTheme,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedTrackColor = SC4
                                    )
                                )
                            }
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Info,
                            title = "Version",
                            trailingText = BuildConfig.VERSION_NAME,
                            onClick = null
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Code,
                            title = "Open Source Agent",
                            trailingText = "GitHub",
                            onClick = {}
                        )
                    )
                )
            }

            // Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { /* replay onboarding */ }) {
                        Text(
                            "Replay onboarding",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            title = { Text("Refresh Interval") },
            text = {
                Column {
                    listOf(2 to "2 seconds", 5 to "5 seconds", 10 to "10 seconds", 30 to "30 seconds")
                        .forEach { (secs, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.refreshInterval == secs,
                                    onClick = {
                                        viewModel.setRefreshInterval(secs)
                                        showRefreshDialog = false
                                    }
                                )
                                Text(label)
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRefreshDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileCard(onQrClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = SC2,
        border = BorderStroke(1.dp, OutlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "AD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Admin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "admin@servercontrol",
                    fontFamily = MonoFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.QrCode2,
                contentDescription = "QR",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onQrClick)
            )
        }
    }
}

private data class SettingRowData(
    val icon: ImageVector,
    val title: String,
    val trailingText: String? = null,
    val onClick: (() -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null
)

@Composable
private fun SettingsSection(label: String, items: List<SettingRowData>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = SC2,
            border = BorderStroke(1.dp, OutlineVariant)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingRow(item)
                    if (index < items.size - 1) {
                        HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(data: SettingRowData) {
    if (data.onClick != null) {
        Surface(
            onClick = data.onClick,
            color = SC2,
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingRowContent(data)
        }
    } else {
        SettingRowContent(data)
    }
}

@Composable
private fun SettingRowContent(data: SettingRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            data.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            data.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        data.trailing?.invoke() ?: run {
            data.trailingText?.let { text ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (data.onClick != null) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
