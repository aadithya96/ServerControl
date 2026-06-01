package com.servercontrol.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
    onReplayOnboarding: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showRefreshDialog by remember { mutableStateOf(false) }
    var showCpuAlertDialog by remember { mutableStateOf(false) }
    var showDiskAlertDialog by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setAlertNotificationsEnabled(granted) }

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
                ProfileCard(
                    displayName = state.profileDisplayName,
                    onDisplayNameChange = viewModel::setProfileDisplayName,
                    onQrClick = onQrTransfer
                )
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
                                    checked = state.alertNotificationsEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.setAlertNotificationsEnabled(enabled)
                                        }
                                    },
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
                            title = "CPU Alerts",
                            trailing = {
                                Switch(
                                    checked = state.cpuAlertEnabled,
                                    onCheckedChange = viewModel::setCpuAlertEnabled,
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
                            enabled = state.cpuAlertEnabled,
                            onClick = if (state.cpuAlertEnabled) ({ showCpuAlertDialog = true }) else null
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Storage,
                            title = "Disk Alerts",
                            trailing = {
                                Switch(
                                    checked = state.diskAlertEnabled,
                                    onCheckedChange = viewModel::setDiskAlertEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedTrackColor = SC4
                                    )
                                )
                            }
                        ),
                        SettingRowData(
                            icon = Icons.Filled.Storage,
                            title = "Disk Alert Threshold",
                            trailingText = "${state.diskAlertThreshold}%",
                            enabled = state.diskAlertEnabled,
                            onClick = if (state.diskAlertEnabled) ({ showDiskAlertDialog = true }) else null
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
                                    checked = state.biometricLockEnabled,
                                    onCheckedChange = viewModel::setBiometricLockEnabled,
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
                                    checked = state.vpnDetectionEnabled,
                                    onCheckedChange = viewModel::setVpnDetectionEnabled,
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
                    TextButton(onClick = onReplayOnboarding) {
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
        ModalBottomSheet(onDismissRequest = { showRefreshDialog = false }) {
            PickerSheetContent(
                title = "Refresh Interval",
                onDismiss = { showRefreshDialog = false }
            ) {
                listOf(2 to "2 seconds", 5 to "5 seconds", 10 to "10 seconds", 30 to "30 seconds")
                    .forEach { (secs, label) ->
                        val selected = state.refreshInterval == secs
                        PickerOption(
                            label = label,
                            selected = selected,
                            onClick = { viewModel.setRefreshInterval(secs); showRefreshDialog = false }
                        )
                    }
            }
        }
    }

    if (showCpuAlertDialog) {
        ModalBottomSheet(onDismissRequest = { showCpuAlertDialog = false }) {
            PickerSheetContent(
                title = "CPU Alert Threshold",
                onDismiss = { showCpuAlertDialog = false }
            ) {
                listOf(50, 70, 80, 90).forEach { pct ->
                    val selected = state.cpuAlertThreshold == pct
                    PickerOption(
                        label = "$pct%",
                        selected = selected,
                        onClick = { viewModel.setCpuAlertThreshold(pct); showCpuAlertDialog = false }
                    )
                }
            }
        }
    }

    if (showDiskAlertDialog) {
        ModalBottomSheet(onDismissRequest = { showDiskAlertDialog = false }) {
            PickerSheetContent(
                title = "Disk Alert Threshold",
                onDismiss = { showDiskAlertDialog = false }
            ) {
                listOf(70, 80, 90, 95).forEach { pct ->
                    val selected = state.diskAlertThreshold == pct
                    PickerOption(
                        label = "$pct%",
                        selected = selected,
                        onClick = { viewModel.setDiskAlertThreshold(pct); showDiskAlertDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerSheetContent(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        content()
    }
}

@Composable
private fun PickerOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.secondary else OutlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onQrClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val initials = displayName.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2).joinToString("")
        .ifBlank { "SC" }

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
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initials,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Display name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
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
    val trailing: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true
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
    if (data.onClick != null && data.enabled) {
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
    val contentAlpha = if (data.enabled) 1f else 0.38f
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
            modifier = Modifier.size(22.dp)
        )
        Text(
            data.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    )
                    if (data.onClick != null && data.enabled) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
