package com.servercontrol.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.presentation.theme.Green80
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_config")

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var serverRepository: ServerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If no valid ID, just cancel
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setContent {
            MaterialTheme {
                WidgetConfigContent(
                    onServerSelected = { server ->
                        lifecycleScope.launch {
                            // Save widget-server association
                            widgetDataStore.edit { prefs ->
                                prefs[longPreferencesKey("widget_${appWidgetId}_server_id")] = server.id
                                prefs[stringPreferencesKey("widget_${appWidgetId}_server_name")] = server.name
                            }

                            // Update the widget state
                            val glanceId = try {
                                androidx.glance.appwidget.GlanceAppWidgetManager(this@WidgetConfigActivity)
                                    .getGlanceIdBy(appWidgetId)
                            } catch (e: Exception) { null }

                            glanceId?.let { id ->
                                androidx.glance.appwidget.state.updateAppWidgetState(
                                    context = this@WidgetConfigActivity,
                                    definition = androidx.glance.state.PreferencesGlanceStateDefinition,
                                    id = id
                                ) { prefs ->
                                    prefs.toMutablePreferences().apply {
                                        this[stringPreferencesKey("widget_server_name")] = server.name
                                    }
                                }
                            }

                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(RESULT_OK, resultValue)
                            finish()
                        }
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }

    @Composable
    private fun WidgetConfigContent(
        onServerSelected: (ServerProfile) -> Unit,
        onCancel: () -> Unit
    ) {
        var servers by remember { mutableStateOf<List<ServerProfile>>(emptyList()) }

        LaunchedEffect(Unit) {
            servers = serverRepository.getAllServers()
        }

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Select Server for Widget") }
                )
            }
        ) { padding ->
            if (servers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text("No servers configured")
                        OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(servers) { server ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onServerSelected(server) }
                        ) {
                            ListItem(
                                headlineContent = { Text(server.name) },
                                supportingContent = { Text("${server.host}:${server.agentPort}") },
                                leadingContent = {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = Green80)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
