package com.servercontrol.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ServerStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ServerStatusWidget()
}
