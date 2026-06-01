package com.servercontrol.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servercontrol.presentation.theme.SC4

@Composable
fun Meter(
    value: Float,
    fillColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 5.dp,
    trackColor: Color = SC4
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "meter"
    )
    Canvas(modifier = modifier.height(barHeight)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        if (animated > 0f) {
            drawRoundRect(
                color = fillColor,
                size = Size(size.width * animated, size.height),
                cornerRadius = radius
            )
        }
    }
}
