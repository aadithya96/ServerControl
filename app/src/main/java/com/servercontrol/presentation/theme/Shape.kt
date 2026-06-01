package com.servercontrol.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(10.dp),       // chips, small tiles
    medium = RoundedCornerShape(14.dp),      // well items, field rows
    large = RoundedCornerShape(20.dp),       // cards
    extraLarge = RoundedCornerShape(50),     // pill, FAB label, full radius
)

// Convenience aliases
val CardShape = RoundedCornerShape(20.dp)
val WellShape = RoundedCornerShape(14.dp)
val ChipShape = RoundedCornerShape(10.dp)
val PillShape = RoundedCornerShape(50)
