package com.servercontrol.presentation.commands

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.SavedCommand
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCommandsScreen(
    serverId: Long,
    onNavigateBack: () -> Unit,
    viewModel: QuickCommandsViewModel = hiltViewModel()
) {
    val commands by viewModel.commands.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val runResult by viewModel.runResult.collectAsState()

    val builtIns = commands.filter { it.isBuiltIn }
    val custom = commands.filter { !it.isBuiltIn }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Commands") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::exportCommands) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add command")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search commands…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            if (builtIns.isNotEmpty()) {
                item {
                    Text(
                        "Built-in",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(builtIns, key = { it.id }) { cmd ->
                    CommandCard(command = cmd, onRun = { viewModel.runCommand(cmd) }, onDelete = null)
                }
            }

            if (custom.isNotEmpty()) {
                item {
                    Text(
                        "Custom",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                items(custom, key = { it.id }) { cmd ->
                    CommandCard(
                        command = cmd,
                        onRun = { viewModel.runCommand(cmd) },
                        onDelete = { viewModel.deleteCommand(cmd.id) }
                    )
                }
            }

            if (commands.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No commands yet. Tap + to add one.")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCommandDialog(
            name = viewModel.newName,
            command = viewModel.newCommand,
            description = viewModel.newDescription,
            onNameChange = { viewModel.newName = it },
            onCommandChange = { viewModel.newCommand = it },
            onDescriptionChange = { viewModel.newDescription = it },
            onSave = viewModel::saveCommand,
            onDismiss = { viewModel.showAddDialog.value = false }
        )
    }

    runResult?.let { (name, result) ->
        CommandOutputBottomSheet(
            commandName = name,
            result = result,
            onDismiss = viewModel::clearResult
        )
    }
}

@Composable
private fun CommandCard(
    command: SavedCommand,
    onRun: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(command.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (command.description.isNotBlank()) {
                    Text(
                        command.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            command.command,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                )
            }
            IconButton(onClick = onRun) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = MaterialTheme.colorScheme.primary)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandOutputBottomSheet(
    commandName: String,
    result: Resource<String>,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(commandName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                when (result) {
                    is Resource.Success -> Badge(containerColor = Color(0xFF4CAF50)) { Text("0") }
                    is Resource.Error -> Badge(containerColor = MaterialTheme.colorScheme.error) { Text("err") }
                    is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }

            when (result) {
                is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Success -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { clipboard.setText(AnnotatedString(result.data)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                    Surface(
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            result.data.lines().forEach { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFE0E0E0)
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Text("Error: ${result.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AddCommandDialog(
    name: String,
    command: String,
    description: String,
    onNameChange: (String) -> Unit,
    onCommandChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Command") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = command,
                    onValueChange = onCommandChange,
                    label = { Text("Command") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank() && command.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
