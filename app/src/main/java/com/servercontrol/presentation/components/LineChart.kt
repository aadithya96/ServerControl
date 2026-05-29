package com.servercontrol.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.2f),
    showDots: Boolean = false,
    yAxisLabel: (Float) -> String = { "${it.toInt()}%" }
) {
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val paddingLeft = 40f
        val paddingRight = 16f
        val paddingTop = 8f
        val paddingBottom = 24f
        val chartW = size.width - paddingLeft - paddingRight
        val chartH = size.height - paddingTop - paddingBottom

        if (chartW <= 0 || chartH <= 0) return@Canvas

        val minVal = 0f
        val maxVal = 100f
        val range = maxVal - minVal

        fun xOf(i: Int): Float = paddingLeft + i.toFloat() / (data.size - 1).coerceAtLeast(1) * chartW
        fun yOf(v: Float): Float = paddingTop + chartH - ((v - minVal) / range * chartH).coerceIn(0f, chartH)

        // Grid lines at 25, 50, 75
        listOf(25f, 50f, 75f).forEach { pct ->
            val y = yOf(pct)
            drawLine(gridColor, Offset(paddingLeft, y), Offset(paddingLeft + chartW, y), strokeWidth = 1f)
            drawText(
                textMeasurer, yAxisLabel(pct), Offset(0f, y - 7f), style = labelStyle
            )
        }

        if (data.size < 2) return@Canvas

        // Build smooth bezier path for fill
        val fillPath = buildSmoothPath(data, ::xOf, ::yOf)
        // Close for fill
        val closedFill = Path().apply {
            addPath(fillPath)
            lineTo(xOf(data.size - 1), paddingTop + chartH)
            lineTo(paddingLeft, paddingTop + chartH)
            close()
        }

        // Draw fill
        drawPath(closedFill, fillColor)

        // Draw line
        drawPath(fillPath, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Dots and last value
        if (showDots) {
            data.forEachIndexed { i, v ->
                drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(xOf(i), yOf(v)))
            }
        }

        // Last value dot + label
        val lastVal = data.last()
        val lastX = xOf(data.size - 1)
        val lastY = yOf(lastVal)
        drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        drawText(
            textMeasurer,
            yAxisLabel(lastVal),
            Offset(lastX - 20f, lastY - 20f),
            style = TextStyle(fontSize = 10.sp, color = lineColor)
        )
    }
}

private fun buildSmoothPath(
    data: List<Float>,
    xOf: (Int) -> Float,
    yOf: (Float) -> Float
): Path {
    val path = Path()
    if (data.isEmpty()) return path

    path.moveTo(xOf(0), yOf(data[0]))
    if (data.size == 1) return path

    for (i in 1 until data.size) {
        val prevX = xOf(i - 1)
        val prevY = yOf(data[i - 1])
        val currX = xOf(i)
        val currY = yOf(data[i])
        val cp1x = prevX + (currX - prevX) * 0.5f
        val cp2x = currX - (currX - prevX) * 0.5f
        path.cubicTo(cp1x, prevY, cp2x, currY, currX, currY)
    }
    return path
}
