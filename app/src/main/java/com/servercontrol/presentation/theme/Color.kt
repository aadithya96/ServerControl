package com.servercontrol.presentation.theme

import androidx.compose.ui.graphics.Color

val Green80 = Color(0xFF1DB954)
val Green40 = Color(0xFF00701A)
val Teal80 = Color(0xFF03DAC5)
val Teal40 = Color(0xFF00857C)
val Red80 = Color(0xFFCF6679)
val Red40 = Color(0xFF9B374D)

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)

// Status colors
val CpuGood = Color(0xFF4CAF50)
val CpuWarn = Color(0xFFFFC107)
val CpuCritical = Color(0xFFF44336)

fun cpuColor(percent: Double): Color = when {
    percent >= 80.0 -> CpuCritical
    percent >= 60.0 -> CpuWarn
    else -> CpuGood
}
