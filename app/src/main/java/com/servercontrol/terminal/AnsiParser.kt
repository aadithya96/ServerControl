package com.servercontrol.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object AnsiParser {

    // ESC sequence regex: ESC [ ... m  or  ESC [ ... H/J/K/etc
    private val escapeRegex = Regex("\\[([0-9;]*)([A-Za-z])")

    fun parse(raw: String, colors: TerminalColorScheme = TerminalThemes.DARK): AnnotatedString {
        // Strip clear screen sequences and handle cursor movement by stripping them
        val cleaned = raw
            .replace("[2J", "")
            .replace("[H", "")
            .replace("[3J", "")

        val ansiColors = mapOf(
            30 to colors.black,
            31 to colors.red,
            32 to colors.green,
            33 to colors.yellow,
            34 to colors.blue,
            35 to colors.magenta,
            36 to colors.cyan,
            37 to colors.white,
            90 to colors.brightBlack,
            91 to colors.brightRed,
            92 to colors.brightGreen,
            93 to colors.brightYellow,
            94 to colors.brightBlue,
            95 to colors.brightMagenta,
            96 to colors.brightCyan,
            97 to colors.brightWhite,
        )

        val bgColors = mapOf(
            40 to colors.black,
            41 to colors.red,
            42 to colors.green,
            43 to colors.yellow,
            44 to colors.blue,
            45 to colors.magenta,
            46 to colors.cyan,
            47 to colors.white,
        )

        return buildAnnotatedString {
            var bold = false
            var italic = false
            var underline = false
            var fgColor: Color? = null
            var bgColor: Color? = null

            var lastEnd = 0
            val matches = escapeRegex.findAll(cleaned)

            for (match in matches) {
                val textBefore = cleaned.substring(lastEnd, match.range.first)
                if (textBefore.isNotEmpty()) {
                    val style = buildSpanStyle(bold, italic, underline, fgColor, bgColor)
                    if (style != null) {
                        pushStyle(style)
                        append(textBefore)
                        pop()
                    } else {
                        append(textBefore)
                    }
                }
                lastEnd = match.range.last + 1

                val command = match.groupValues[2]
                val params = match.groupValues[1]

                if (command == "m") {
                    // SGR - Select Graphic Rendition
                    if (params.isEmpty() || params == "0") {
                        bold = false
                        italic = false
                        underline = false
                        fgColor = null
                        bgColor = null
                    } else {
                        val codes = params.split(";").mapNotNull { it.toIntOrNull() }
                        for (code in codes) {
                            when {
                                code == 0 -> {
                                    bold = false; italic = false; underline = false
                                    fgColor = null; bgColor = null
                                }
                                code == 1 -> bold = true
                                code == 3 -> italic = true
                                code == 4 -> underline = true
                                code == 22 -> bold = false
                                code == 23 -> italic = false
                                code == 24 -> underline = false
                                code in 30..37 -> fgColor = ansiColors[code]
                                code in 90..97 -> fgColor = ansiColors[code]
                                code == 39 -> fgColor = null
                                code in 40..47 -> bgColor = bgColors[code]
                                code == 49 -> bgColor = null
                            }
                        }
                    }
                }
                // All other escape sequences (cursor movement, etc.) are consumed/ignored
            }

            // Remaining text after last escape
            val remaining = cleaned.substring(lastEnd)
            if (remaining.isNotEmpty()) {
                val style = buildSpanStyle(bold, italic, underline, fgColor, bgColor)
                if (style != null) {
                    pushStyle(style)
                    append(remaining)
                    pop()
                } else {
                    append(remaining)
                }
            }
        }
    }

    private fun buildSpanStyle(
        bold: Boolean,
        italic: Boolean,
        underline: Boolean,
        fgColor: Color?,
        bgColor: Color?
    ): SpanStyle? {
        if (!bold && !italic && !underline && fgColor == null && bgColor == null) return null
        return SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = if (underline) TextDecoration.Underline else null,
            color = fgColor ?: Color.Unspecified,
            background = bgColor ?: Color.Unspecified
        )
    }
}
