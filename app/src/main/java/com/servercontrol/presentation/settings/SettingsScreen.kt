package com.servercontrol.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance
            ListItem(
                headlineContent = { Text("Dark Theme") },
                supportingContent = { Text("Use dark color scheme") },
                trailingContent = {
                    Switch(checked = state.darkTheme, onCheckedChange = viewModel::setDarkTheme)
                }
            )
            HorizontalDivider()

            // Refresh interval
            ListItem(
                headlineContent = { Text("Default Refresh Interval") },
                supportingContent = { Text("${state.refreshIntervalSeconds}s") }
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 5, 10, 30).forEach { secs ->
                    FilterChip(
                        selected = state.refreshIntervalSeconds == secs,
                        onClick = { viewModel.setRefreshInterval(secs) },
                        label = { Text("${secs}s") }
                    )
                }
            }
            HorizontalDivider()

            // CPU alert threshold
            ListItem(
                headlineContent = { Text("CPU Alert Threshold") },
                supportingContent = { Text("Alert when CPU > ${"%.0f".format(state.cpuAlertThreshold)}%") }
            )
            Slider(
                value = state.cpuAlertThreshold,
                onValueChange = viewModel::setCpuAlertThreshold,
                valueRange = 50f..100f,
                steps = 9,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider()

            // Disk alert threshold
            ListItem(
                headlineContent = { Text("Disk Alert Threshold") },
                supportingContent = { Text("Alert when disk > ${"%.0f".format(state.diskAlertThreshold)}%") }
            )
            Slider(
                value = state.diskAlertThreshold,
                onValueChange = viewModel::setDiskAlertThreshold,
                valueRange = 50f..100f,
                steps = 9,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider()

            // Background monitoring
            ListItem(
                headlineContent = { Text("Background Monitoring") },
                supportingContent = { Text("Periodic checks even when app is closed") },
                trailingContent = {
                    Switch(
                        checked = state.backgroundMonitoring,
                        onCheckedChange = viewModel::setBackgroundMonitoring
                    )
                }
            )
            HorizontalDivider()

            // About
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }
}
