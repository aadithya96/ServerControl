package com.servercontrol.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.servercontrol.MainActivity

class ServerMetricsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            ServerMetricsWidgetContent()
        }
    }
}

@Composable
fun ServerMetricsWidgetContent() {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
    val serverName = prefs[stringPreferencesKey("widget_server_name")] ?: "No server"
    val cpuPercent = (prefs[floatPreferencesKey("widget_cpu_percent")] ?: 0f) / 100f
    val ramPercent = (prefs[floatPreferencesKey("widget_ram_percent")] ?: 0f) / 100f
    val diskPercent = (prefs[floatPreferencesKey("widget_disk_percent")] ?: 0f) / 100f

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1E1E1E)))
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Text(
                text = serverName,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            MetricBar(label = "CPU", progress = cpuPercent.coerceIn(0f, 1f))
            Spacer(modifier = GlanceModifier.height(6.dp))
            MetricBar(label = "RAM", progress = ramPercent.coerceIn(0f, 1f))
            Spacer(modifier = GlanceModifier.height(6.dp))
            MetricBar(label = "Dsk", progress = diskPercent.coerceIn(0f, 1f))
        }
    }
}

@Composable
private fun MetricBar(label: String, progress: Float) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA))),
            modifier = GlanceModifier.width(28.dp)
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.defaultWeight(),
            color = ColorProvider(barColor(progress)),
            backgroundColor = ColorProvider(Color(0xFF3A3A3A))
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = "${"%.0f".format(progress * 100)}%",
            style = TextStyle(color = ColorProvider(Color(0xFFAAAAAA)))
        )
    }
}

private fun barColor(progress: Float): Color = when {
    progress >= 0.80f -> Color(0xFFF44336)
    progress >= 0.60f -> Color(0xFFFFC107)
    else -> Color(0xFF4CAF50)
}
