package com.servercontrol.presentation.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.AuditLogEntry
import com.servercontrol.domain.model.FailedLoginAttempt
import com.servercontrol.domain.model.SslCertInfo
import com.servercontrol.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val failedLogins by viewModel.failedLogins.collectAsState()
    val sslCerts by viewModel.sslCerts.collectAsState()
    val auditLog by viewModel.auditLogDisplay.collectAsState()
    val blockResult by viewModel.blockResult.collectAsState()
    val domainsInput by viewModel.domainsInput.collectAsState()
    val showAllServers by viewModel.showAllServersAudit.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(blockResult) {
        when (val r = blockResult) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(r.data)
                viewModel.clearBlockResult()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar("Error: ${r.message}")
                viewModel.clearBlockResult()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Security") },
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
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { viewModel.selectedTab.value = 0 }) {
                    Text("Failed Logins", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { viewModel.selectedTab.value = 1 }) {
                    Text("SSL Certs", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { viewModel.selectedTab.value = 2 }) {
                    Text("Audit Log", modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp))
                }
            }

            when (selectedTab) {
                0 -> FailedLoginsTab(failedLogins = failedLogins, onBlockIp = { viewModel.blockIp(it) })
                1 -> SslCertsTab(
                    domains = domainsInput,
                    onDomainsChange = { viewModel.domainsInput.value = it },
                    onCheck = viewModel::checkSslCerts,
                    certs = sslCerts
                )
                2 -> AuditLogTab(
                    entries = auditLog,
                    showAll = showAllServers,
                    onToggleFilter = { viewModel.showAllServersAudit.value = !showAllServers }
                )
            }
        }
    }
}

@Composable
private fun FailedLoginsTab(
    failedLogins: Resource<List<FailedLoginAttempt>>,
    onBlockIp: (String) -> Unit
) {
    when (failedLogins) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: ${failedLogins.message}", color = MaterialTheme.colorScheme.error)
        }
        is Resource.Success -> {
            val data = failedLogins.data
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SummaryCard(
                        uniqueIps = data.map { it.sourceIp }.distinct().size,
                        totalAttempts = data.sumOf { it.count }
                    )
                }
                if (data.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No failed login attempts found")
                        }
                    }
                } else {
                    items(data) { attempt ->
                        FailedLoginRow(attempt = attempt, onBlockIp = onBlockIp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(uniqueIps: Int, totalAttempts: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$uniqueIps", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Unique IPs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VerticalDivider(modifier = Modifier.height(40.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$totalAttempts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Total Attempts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FailedLoginRow(attempt: FailedLoginAttempt, onBlockIp: (String) -> Unit) {
    var showBlockDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attempt.sourceIp,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "User: ${attempt.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (attempt.timestamp.isNotBlank()) {
                    Text(
                        "Last seen: ${attempt.timestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text("${attempt.count}")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { showBlockDialog = true }) {
                Icon(Icons.Default.Shield, contentDescription = "Block IP", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block IP") },
            text = { Text("Block ${attempt.sourceIp} via iptables? This will drop all incoming traffic from this IP.") },
            confirmButton = {
                TextButton(onClick = { onBlockIp(attempt.sourceIp); showBlockDialog = false }) {
                    Text("Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SslCertsTab(
    domains: String,
    onDomainsChange: (String) -> Unit,
    onCheck: () -> Unit,
    certs: Resource<List<SslCertInfo>>?
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = domains,
            onValueChange = onDomainsChange,
            label = { Text("Domains") },
            placeholder = { Text("e.g. example.com, api.example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Security, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Check SSL Certificates")
        }

        when (certs) {
            null -> {}
            is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is Resource.Error -> Text("Error: ${certs.message}", color = MaterialTheme.colorScheme.error)
            is Resource.Success -> {
                certs.data.forEach { cert ->
                    SslCertCard(cert = cert)
                }
                if (certs.data.isEmpty()) {
                    Text("No certificate data returned", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SslCertCard(cert: SslCertInfo) {
    val chipColor = when {
        !cert.isValid || cert.daysUntilExpiry <= 0 -> MaterialTheme.colorScheme.error
        cert.daysUntilExpiry <= 10 -> Color(0xFFF44336)
        cert.daysUntilExpiry <= 30 -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!cert.isValid) {
                Icon(Icons.Default.Error, contentDescription = "Invalid", tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(cert.domain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (cert.issuer.isNotBlank()) {
                    Text(cert.issuer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val expiryText = if (cert.expiryDate > 0) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    "Expires: ${sdf.format(Date(cert.expiryDate))}"
                } else ""
                if (expiryText.isNotBlank()) {
                    Text(expiryText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Badge(containerColor = chipColor) {
                Text(if (cert.daysUntilExpiry > 0) "${cert.daysUntilExpiry}d" else "Expired")
            }
        }
    }
}

@Composable
private fun AuditLogTab(
    entries: List<AuditLogEntry>,
    showAll: Boolean,
    onToggleFilter: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show all servers", modifier = Modifier.weight(1f))
            Switch(checked = showAll, onCheckedChange = { onToggleFilter() })
        }
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No audit log entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(entries) { entry ->
                    AuditLogRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun AuditLogRow(entry: AuditLogEntry) {
    val sdf = remember { SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    entry.action,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Badge(
                    containerColor = if (entry.result == "success") Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                ) {
                    Text(entry.result)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(entry.serverName, style = MaterialTheme.typography.labelSmall) }
                )
                Text(
                    sdf.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.details.isNotBlank()) {
                Text(
                    entry.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
