package com.servercontrol.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.servercontrol.MainActivity

class ServerStatusWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ServerStatusWidgetContent(context)
        }
    }
}

@Composable
fun ServerStatusWidgetContent(context: Context) {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
    val serverName = prefs[stringPreferencesKeyCompat("widget_server_name")] ?: "No server"
    val cpuPercent = prefs[floatPreferencesKeyCompat("widget_cpu_percent")] ?: -1f
    val isOnline = prefs[boolPreferencesKeyCompat("widget_is_online")] ?: false

    val statusColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)

    GlanceTheme {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1E1E1E)))
                .padding(horizontal = 12.dp_glance, vertical = 8.dp_glance)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Box(
                modifier = GlanceModifier
                    .size(10.dp_glance)
                    .background(ColorProvider(statusColor))
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp_glance))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = serverName,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                if (cpuPercent >= 0) {
                    Text(
                        text = "CPU: ${"%.0f".format(cpuPercent)}%",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFAAAAAA))
                        )
                    )
                }
            }
        }
    }
}

// Helpers for Glance dp (Glance does not use Compose dp directly in the same way for layout)
val Int.dp_glance get() = androidx.glance.unit.Dp(this.toFloat())

fun stringPreferencesKeyCompat(name: String) = androidx.datastore.preferences.core.stringPreferencesKey(name)
fun floatPreferencesKeyCompat(name: String) = androidx.datastore.preferences.core.floatPreferencesKey(name)
fun boolPreferencesKeyCompat(name: String) = androidx.datastore.preferences.core.booleanPreferencesKey(name)
