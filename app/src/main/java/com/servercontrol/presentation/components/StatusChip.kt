package com.servercontrol.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servercontrol.presentation.theme.ChipShape
import com.servercontrol.presentation.theme.LocalStatusColors
import com.servercontrol.presentation.theme.StatusInfoColor

enum class ServerStatus { ONLINE, WARN, DOWN, UNKNOWN }

@Composable
fun StatusChip(
    status: ServerStatus,
    modifier: Modifier = Modifier
) {
    val sc = LocalStatusColors.current
    val (label, fg, bg) = when (status) {
        ServerStatus.ONLINE  -> Triple("Online",   sc.online, sc.onlineBg)
        ServerStatus.WARN    -> Triple("Degraded", sc.warn,   sc.warnBg)
        ServerStatus.DOWN    -> Triple("Offline",  sc.down,   sc.downBg)
        ServerStatus.UNKNOWN -> Triple("Unknown",  StatusInfoColor, Color(0xFF1A2333))
    }
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(
            text = label,
            color = fg,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp
        )
    }
}
