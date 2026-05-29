package com.servercontrol.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ServerMetricsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ServerMetricsWidget()
}
