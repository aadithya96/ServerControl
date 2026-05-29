package com.servercontrol.terminal

import androidx.compose.ui.graphics.Color
import com.servercontrol.presentation.terminal.TerminalColorTheme

data class TerminalColorScheme(
    val background: Color,
    val foreground: Color,
    val cursor: Color,
    val selectionBg: Color,
    val black: Color,
    val red: Color,
    val green: Color,
    val yellow: Color,
    val blue: Color,
    val magenta: Color,
    val cyan: Color,
    val white: Color,
    val brightBlack: Color,
    val brightRed: Color,
    val brightGreen: Color,
    val brightYellow: Color,
    val brightBlue: Color,
    val brightMagenta: Color,
    val brightCyan: Color,
    val brightWhite: Color
)

object TerminalThemes {
    val DARK = TerminalColorScheme(
        background = Color(0xFF0D1117),
        foreground = Color(0xFFE6EDF3),
        cursor = Color(0xFF58A6FF),
        selectionBg = Color(0xFF264F78),
        black = Color(0xFF161B22), red = Color(0xFFFF7B72),
        green = Color(0xFF3FB950), yellow = Color(0xFFD29922),
        blue = Color(0xFF58A6FF), magenta = Color(0xFFBC8CFF),
        cyan = Color(0xFF39C5CF), white = Color(0xFFB1BAC4),
        brightBlack = Color(0xFF6E7681), brightRed = Color(0xFFFFA198),
        brightGreen = Color(0xFF56D364), brightYellow = Color(0xFFE3B341),
        brightBlue = Color(0xFF79C0FF), brightMagenta = Color(0xFFD2A8FF),
        brightCyan = Color(0xFF56D4DD), brightWhite = Color(0xFFECF2F8)
    )

    val SOLARIZED = TerminalColorScheme(
        background = Color(0xFF002B36),
        foreground = Color(0xFF839496),
        cursor = Color(0xFF268BD2),
        selectionBg = Color(0xFF073642),
        black = Color(0xFF073642), red = Color(0xFFDC322F),
        green = Color(0xFF859900), yellow = Color(0xFFB58900),
        blue = Color(0xFF268BD2), magenta = Color(0xFFD33682),
        cyan = Color(0xFF2AA198), white = Color(0xFFEEE8D5),
        brightBlack = Color(0xFF002B36), brightRed = Color(0xFFCB4B16),
        brightGreen = Color(0xFF586E75), brightYellow = Color(0xFF657B83),
        brightBlue = Color(0xFF839496), brightMagenta = Color(0xFF6C71C4),
        brightCyan = Color(0xFF93A1A1), brightWhite = Color(0xFFFDF6E3)
    )

    val DRACULA = TerminalColorScheme(
        background = Color(0xFF282A36),
        foreground = Color(0xFFF8F8F2),
        cursor = Color(0xFF50FA7B),
        selectionBg = Color(0xFF44475A),
        black = Color(0xFF21222C), red = Color(0xFFFF5555),
        green = Color(0xFF50FA7B), yellow = Color(0xFFF1FA8C),
        blue = Color(0xFFBD93F9), magenta = Color(0xFFFF79C6),
        cyan = Color(0xFF8BE9FD), white = Color(0xFFF8F8F2),
        brightBlack = Color(0xFF6272A4), brightRed = Color(0xFFFF6E6E),
        brightGreen = Color(0xFF69FF94), brightYellow = Color(0xFFFFFF87),
        brightBlue = Color(0xFFD6ACFF), brightMagenta = Color(0xFFFF92DF),
        brightCyan = Color(0xFFA4FFFF), brightWhite = Color(0xFFFFFFFF)
    )

    val LIGHT = TerminalColorScheme(
        background = Color(0xFFFFFFFF),
        foreground = Color(0xFF24292E),
        cursor = Color(0xFF0366D6),
        selectionBg = Color(0xFFB3D7FF),
        black = Color(0xFF24292E), red = Color(0xFFD73A49),
        green = Color(0xFF22863A), yellow = Color(0xFFB08800),
        blue = Color(0xFF0366D6), magenta = Color(0xFF6F42C1),
        cyan = Color(0xFF1B7C83), white = Color(0xFF6A737D),
        brightBlack = Color(0xFF959DA5), brightRed = Color(0xFFCB2431),
        brightGreen = Color(0xFF28A745), brightYellow = Color(0xFFDBB915),
        brightBlue = Color(0xFF2188FF), brightMagenta = Color(0xFF8A63D2),
        brightCyan = Color(0xFF3192AA), brightWhite = Color(0xFFD1D5DA)
    )

    fun forTheme(theme: TerminalColorTheme) = when (theme) {
        TerminalColorTheme.DARK -> DARK
        TerminalColorTheme.SOLARIZED -> SOLARIZED
        TerminalColorTheme.DRACULA -> DRACULA
        TerminalColorTheme.LIGHT -> LIGHT
    }
}
