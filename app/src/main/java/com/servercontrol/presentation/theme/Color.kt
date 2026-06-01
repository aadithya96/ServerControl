package com.servercontrol.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Emerald M3 dark scheme — seed #1DB954
val Primary = Color(0xFF6FDDA6)
val OnPrimary = Color(0xFF003824)
val PrimaryContainer = Color(0xFF005138)
val OnPrimaryContainer = Color(0xFF8DFAC1)

val Secondary = Color(0xFFB4CCBD)
val SecondaryContainer = Color(0xFF22312A)
val OnSecondaryContainer = Color(0xFFCFE9D8)

val Tertiary = Color(0xFFA6CCDC)

val Background = Color(0xFF0E1411)
val Surface = Color(0xFF0E1411)
val OnSurface = Color(0xFFDEE4DE)
val OnSurfaceVariant = Color(0xFFBEC9C0)

val Outline = Color(0xFF88938B)
val OutlineVariant = Color(0xFF3E4843)

val ErrorColor = Color(0xFFFFB4AB)
val ErrorContainerColor = Color(0xFF93000A)
val OnErrorColor = Color(0xFF690005)

// Surface containers (sc1..sc4)
val SC1 = Color(0xFF151B18)   // surfaceContainerLowest
val SC2 = Color(0xFF19211D)   // surfaceContainer
val SC3 = Color(0xFF1F2723)   // surfaceContainerHigh
val SC4 = Color(0xFF28302C)   // surfaceContainerHighest

// Status palette — semantic, fixed, never swapped with seed
val StatusOnlineColor = Color(0xFF7FD894)
val StatusOnlineBg = Color(0xFF0C2B16)
val StatusWarnColor = Color(0xFFF1C264)
val StatusWarnBg = Color(0xFF2E2410)
val StatusDownColor = Color(0xFFFFB4AB)
val StatusDownBg = Color(0xFF3B1411)
val StatusInfoColor = Color(0xFFA8C7FF)

data class StatusColors(
    val online: Color = StatusOnlineColor,
    val onlineBg: Color = StatusOnlineBg,
    val warn: Color = StatusWarnColor,
    val warnBg: Color = StatusWarnBg,
    val down: Color = StatusDownColor,
    val downBg: Color = StatusDownBg,
    val info: Color = StatusInfoColor
)

val LocalStatusColors = staticCompositionLocalOf { StatusColors() }

fun cpuColor(percent: Double): Color = when {
    percent >= 80.0 -> StatusWarnColor
    percent >= 60.0 -> StatusWarnColor
    else -> Primary
}

// Legacy names kept for backward-compat with code not yet updated
val Green80 = Primary
val Green40 = PrimaryContainer
val Teal80 = Tertiary
val Teal40 = Color(0xFF00857C)
val Red80 = ErrorColor
val Red40 = ErrorContainerColor
val DarkBackground = Background
val DarkSurface = SC2
val DarkSurfaceVariant = SC3
val CpuGood = StatusOnlineColor
val CpuWarn = StatusWarnColor
val CpuCritical = StatusDownColor
