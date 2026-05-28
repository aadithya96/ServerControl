package com.servercontrol.presentation.firewall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.FirewallRule
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.LoadingSpinner
import com.servercontrol.presentation.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: FirewallViewModel = hiltViewModel()
) {
    LaunchedEffect(serverId) { viewModel.init(serverId) }

    val state by viewModel.uiState.collectAsState()
    var ruleToToggle by remember { mutableStateOf<Pair<FirewallRule, Boolean>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firewall") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Warning: Changes take effect immediately",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            when {
                state.isLoading -> LoadingSpinner()
                state.error != null -> ErrorState(message = state.error!!, onRetry = viewModel::refresh)
                else -> {
                    val grouped = state.rules.groupBy { it.chain }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        grouped.forEach { (chain, rules) ->
                            item { SectionHeader(title = chain) }
                            items(rules, key = { it.id }) { rule ->
                                FirewallRuleRow(
                                    rule = rule,
                                    onToggle = { enabled -> ruleToToggle = rule to enabled }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    ruleToToggle?.let { (rule, enable) ->
        AlertDialog(
            onDismissRequest = { ruleToToggle = null },
            title = { Text(if (enable) "Enable Rule" else "Disable Rule") },
            text = { Text("${if (enable) "Enable" else "Disable"} rule: ${rule.target} ${rule.protocol} ${rule.source} → ${rule.destination}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleRule(rule.id, enable)
                    ruleToToggle = null
                }) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToToggle = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FirewallRuleRow(rule: FirewallRule, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(onClick = {}, label = { Text(rule.target, style = MaterialTheme.typography.labelSmall) })
                AssistChip(onClick = {}, label = { Text(rule.protocol, style = MaterialTheme.typography.labelSmall) })
            }
            Text(
                text = "${rule.source} → ${rule.destination}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (rule.options.isNotBlank()) {
                Text(
                    text = rule.options,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = rule.enabled,
            onCheckedChange = { onToggle(it) }
        )
    }
}
