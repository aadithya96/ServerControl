package com.servercontrol.presentation.servers

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.domain.model.AuthType
import com.servercontrol.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInstaller: (Long) -> Unit = {},
    viewModel: AddServerViewModel = hiltViewModel()
) {
    val saveState by viewModel.saveState.collectAsState()
    val testState by viewModel.testConnectionState.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val pendingInstallServerId by viewModel.pendingInstallServerId.collectAsState()

    LaunchedEffect(saveState, pendingInstallServerId) {
        if (saveState is Resource.Success && pendingInstallServerId == null) onNavigateBack()
    }

    pendingInstallServerId?.let { newServerId ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearInstallPrompt()
                onNavigateBack()
            },
            title = { Text("Install Daemon?") },
            text = {
                Text(
                    "Would you like to install the ServerControl monitoring daemon on this server now? " +
                    "The app will connect via SSH and set it up automatically — no copy-pasting required."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearInstallPrompt()
                    onNavigateToInstaller(newServerId)
                }) { Text("Install Now") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearInstallPrompt()
                    onNavigateBack()
                }) { Text("Later") }
            }
        )
    }

    val isSaving = saveState is Resource.Loading
    val isTesting = testState is Resource.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.editServerId != null) "Edit Server" else "Add Server") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Display Name
            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = { viewModel.displayName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Host / IP
            OutlinedTextField(
                value = viewModel.host,
                onValueChange = { viewModel.host = it },
                label = { Text("Host / IP") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            // Agent Port
            OutlinedTextField(
                value = viewModel.agentPort,
                onValueChange = { viewModel.agentPort = it },
                label = { Text("Agent Port") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Auth Type Segmented Buttons
            Text("Authentication", style = MaterialTheme.typography.titleSmall)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    AuthType.AGENT_TOKEN to "Agent Token",
                    AuthType.SSH_PASSWORD to "SSH Password",
                    AuthType.SSH_KEY to "SSH Key"
                )
                options.forEachIndexed { index, (type, label) ->
                    SegmentedButton(
                        selected = viewModel.authType == type,
                        onClick = { viewModel.authType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            // Agent Token fields
            AnimatedVisibility(visible = viewModel.authType == AuthType.AGENT_TOKEN) {
                var tokenVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = viewModel.agentToken,
                    onValueChange = { viewModel.agentToken = it },
                    label = { Text("Agent Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { tokenVisible = !tokenVisible }) {
                            Icon(
                                if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (tokenVisible) "Hide token" else "Show token"
                            )
                        }
                    }
                )
            }

            // SSH Password fields
            AnimatedVisibility(visible = viewModel.authType == AuthType.SSH_PASSWORD) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.sshUser,
                        onValueChange = { viewModel.sshUser = it },
                        label = { Text("SSH Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    var passVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = viewModel.sshPassword,
                        onValueChange = { viewModel.sshPassword = it },
                        label = { Text("SSH Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(
                                    if (passVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passVisible) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = viewModel.sshPort,
                        onValueChange = { viewModel.sshPort = it },
                        label = { Text("SSH Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // SSH Key fields
            AnimatedVisibility(visible = viewModel.authType == AuthType.SSH_KEY) {
                val context = LocalContext.current
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        // Read key content from URI
                        try {
                            val stream = context.contentResolver.openInputStream(uri)
                            val content = stream?.bufferedReader()?.readText() ?: ""
                            stream?.close()
                            viewModel.sshKeyPath = content
                        } catch (e: Exception) {
                            viewModel.sshKeyPath = uri.toString()
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.sshUser,
                        onValueChange = { viewModel.sshUser = it },
                        label = { Text("SSH Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = if (viewModel.sshKeyPath.length > 80)
                            viewModel.sshKeyPath.take(40) + "..." + viewModel.sshKeyPath.takeLast(20)
                        else viewModel.sshKeyPath,
                        onValueChange = {},
                        label = { Text("Private Key (PEM)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        maxLines = 3,
                        trailingIcon = {
                            IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = viewModel.sshPort,
                        onValueChange = { viewModel.sshPort = it },
                        label = { Text("SSH Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Test connection result (only show outcome, not a duplicate loading indicator)
            when (val ts = testState) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Connected in ${ts.data}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is Resource.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Error: ${ts.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                null -> Unit
            }

            // Save error
            if (saveState is Resource.Error) {
                Text(
                    text = (saveState as Resource.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Test Connection Button
            ElevatedButton(
                onClick = viewModel::testConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTesting && !isSaving
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing...")
                } else {
                    Text("Test Connection")
                }
            }

            // Save Button
            Button(
                onClick = viewModel::saveServer,
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid && !isSaving && !isTesting
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save")
                }
            }
        }
    }
}
