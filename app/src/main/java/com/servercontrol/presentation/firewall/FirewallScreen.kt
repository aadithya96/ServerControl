package com.servercontrol.presentation.firewall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.FirewallChain
import com.servercontrol.domain.model.FirewallRule
import com.servercontrol.presentation.components.ErrorState
import com.servercontrol.presentation.components.ShimmerCard
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: FirewallViewModel = hiltViewModel()
) {
    val firewallResource by viewModel.firewallData.collectAsState()
    val toggleResult by viewModel.toggleResult.collectAsState()
    val expandedChains by viewModel.expandedChains.collectAsState()

    var ruleToToggle by remember { mutableStateOf<Pair<FirewallRule, Boolean>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toggleResult) {
        when (val result = toggleResult) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(result.data)
                viewModel.clearToggleResult()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar("Error: ${result.message}")
                viewModel.clearToggleResult()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firewall") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Warning banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFE65100)
                    )
                    Text(
                        text = "Changes take effect immediately on the server",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                }
            }

            when (val resource = firewallResource) {
                is Resource.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { ShimmerCard() }
                    }
                }
                is Resource.Error -> ErrorState(
                    message = resource.message,
                    onRetry = viewModel::refresh
                )
                is Resource.Success -> {
                    val data = resource.data
                    // Backend chip
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(data.backend) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(data.chains, key = { it.name }) { chain ->
                            ChainCard(
                                chain = chain,
                                isExpanded = chain.name in expandedChains,
                                onToggleExpand = { viewModel.toggleChainExpanded(chain.name) },
                                onToggleRule = { rule, enabled -> ruleToToggle = rule to enabled }
                            )
                        }
                    }
                }
            }
        }
    }

    // Toggle confirmation dialog
    ruleToToggle?.let { (rule, enable) ->
        AlertDialog(
            onDismissRequest = { ruleToToggle = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Toggle Firewall Rule?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("This will ${if (enable) "enable" else "disable"} the following rule:")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Chain: ${rule.chain}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Target: ${rule.target}  Protocol: ${rule.protocol}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${rule.source} → ${rule.destination}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (rule.options.isNotBlank()) {
                        Text(
                            text = rule.options,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleRule(rule.id, enable)
                        ruleToToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToToggle = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ChainCard(
    chain: FirewallChain,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleRule: (FirewallRule, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Chain header
            Surface(
                onClick = onToggleExpand,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = chain.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    // Policy chip
                    val (policyColor, policyTextColor) = when (chain.policy.uppercase()) {
                        "ACCEPT" -> Color(0xFF2E7D32) to Color.White
                        "DROP" -> Color(0xFFC62828) to Color.White
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = policyColor
                    ) {
                        Text(
                            text = chain.policy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = policyTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "${chain.rules.size} rules",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider()
                    chain.rules.forEachIndexed { index, rule ->
                        FirewallRuleRow(ruleNumber = index + 1, rule = rule, onToggle = { onToggleRule(rule, it) })
                        if (index < chain.rules.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                    }
                    if (chain.rules.isEmpty()) {
                        Text(
                            text = "No rules in this chain",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FirewallRuleRow(
    ruleNumber: Int,
    rule: FirewallRule,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Number circle
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = ruleNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Target chip
                val (targetColor, targetTextColor) = when (rule.target.uppercase()) {
                    "ACCEPT" -> Color(0xFF2E7D32) to Color.White
                    "DROP" -> Color(0xFFC62828) to Color.White
                    "REJECT" -> Color(0xFFE65100) to Color.White
                    "LOG" -> Color(0xFF1565C0) to Color.White
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = targetColor
                ) {
                    Text(
                        text = rule.target,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = targetTextColor
                    )
                }
                Text(
                    text = rule.protocol,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${rule.source} → ${rule.destination}",
                style = MaterialTheme.typography.bodySmall
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
