package com.servercontrol.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showRefreshDialog by remember { mutableStateOf(false) }
    var webhookTestResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Appearance ---
            item {
                SettingsSectionHeader("Appearance")
            }
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Theme",
                    subtitle = "Use dark color scheme",
                    trailing = {
                        Switch(
                            checked = state.isDarkTheme,
                            onCheckedChange = viewModel::setDarkTheme
                        )
                    }
                )
            }
            item { HorizontalDivider() }

            // --- Monitoring ---
            item { SettingsSectionHeader("Monitoring") }
            item {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Default Refresh Interval",
                    subtitle = "Currently: ${state.refreshInterval}s",
                    onClick = { showRefreshDialog = true },
                    trailing = {
                        Text(
                            text = "${state.refreshInterval}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Background Monitoring",
                    subtitle = "Periodic checks even when app is closed",
                    trailing = {
                        Switch(
                            checked = state.backgroundMonitoringEnabled,
                            onCheckedChange = viewModel::setBackgroundMonitoringEnabled
                        )
                    }
                )
            }
            if (state.backgroundMonitoringEnabled) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Check every ${state.backgroundMonitoringInterval} minutes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = state.backgroundMonitoringInterval.toFloat(),
                            onValueChange = { viewModel.setBackgroundMonitoringInterval(it.toInt()) },
                            valueRange = 5f..60f,
                            steps = 10,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item { HorizontalDivider() }

            // --- Alerts ---
            item { SettingsSectionHeader("Alerts") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CPU Alert", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Notify when CPU exceeds ${state.cpuAlertThreshold}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Slider(
                        value = state.cpuAlertThreshold.toFloat(),
                        onValueChange = { viewModel.setCpuAlertThreshold(it.toInt()) },
                        valueRange = 50f..100f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Disk Alert", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Notify when disk usage exceeds ${state.diskAlertThreshold}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Slider(
                        value = state.diskAlertThreshold.toFloat(),
                        onValueChange = { viewModel.setDiskAlertThreshold(it.toInt()) },
                        valueRange = 50f..100f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item { HorizontalDivider() }

            // --- Webhook Alerts ---
            item { SettingsSectionHeader("Webhook Alerts") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Webhook Type", style = MaterialTheme.typography.bodyLarge)
                    val webhookTypes = listOf("none" to "None", "slack" to "Slack", "discord" to "Discord", "telegram" to "Telegram")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        webhookTypes.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = state.webhookType == key,
                                onClick = { viewModel.setWebhookType(key) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = webhookTypes.size)
                            ) { Text(label, maxLines = 1) }
                        }
                    }

                    if (state.webhookType == "slack" || state.webhookType == "discord") {
                        OutlinedTextField(
                            value = state.webhookUrl,
                            onValueChange = { viewModel.setWebhookUrl(it) },
                            label = { Text("Webhook URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (state.webhookType == "telegram") {
                        OutlinedTextField(
                            value = state.telegramBotToken,
                            onValueChange = { viewModel.setTelegramBotToken(it) },
                            label = { Text("Bot Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.telegramChatId,
                            onValueChange = { viewModel.setTelegramChatId(it) },
                            label = { Text("Chat ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (state.webhookType != "none") {
                        Button(
                            onClick = { webhookTestResult = "Sending test…"; viewModel.sendTestWebhook() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Test Webhook") }

                        webhookTestResult?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item { HorizontalDivider() }

            // --- About ---
            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = BuildConfig.VERSION_NAME
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Agent",
                    subtitle = BuildConfig.GITHUB_URL
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Refresh interval dialog
    if (showRefreshDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDialog = false },
            title = { Text("Refresh Interval") },
            text = {
                Column {
                    listOf(2 to "2 seconds", 5 to "5 seconds", 10 to "10 seconds", 30 to "30 seconds").forEach { (secs, label) ->
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
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    if (onClick != null) {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            SettingsItemContent(icon, title, subtitle, trailing)
        }
    } else {
        SettingsItemContent(icon, title, subtitle, trailing)
    }
}

@Composable
private fun SettingsItemContent(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    trailing: (@Composable () -> Unit)?
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = trailing
    )
}
