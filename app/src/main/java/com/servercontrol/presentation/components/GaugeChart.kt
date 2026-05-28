package com.servercontrol.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servercontrol.presentation.theme.cpuColor

@Composable
fun GaugeChart(
    percent: Double,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Float = 20f,
    trackColor: Color = Color.Gray.copy(alpha = 0.3f),
    label: String = "CPU"
) {
    val gaugeColor = cpuColor(percent)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val sweepAngle = 240f
            val startAngle = 150f
            val padding = strokeWidth
            val arcSize = Size(
                this.size.width - padding * 2,
                this.size.height - padding * 2
            )
            val topLeft = Offset(padding, padding)

            // Track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Filled arc
            drawArc(
                color = gaugeColor,
                startAngle = startAngle,
                sweepAngle = (sweepAngle * (percent / 100.0)).toFloat().coerceIn(0f, sweepAngle),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "%.0f%%".format(percent),
            style = MaterialTheme.typography.titleLarge,
            color = gaugeColor
        )
    }
}
