package com.servercontrol.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object AnsiParser {

    private val ansiColors = mapOf(
        30 to Color(0xFF000000),
        31 to Color(0xFFFF5555),
        32 to Color(0xFF55FF55),
        33 to Color(0xFFFFFF55),
        34 to Color(0xFF5555FF),
        35 to Color(0xFFFF55FF),
        36 to Color(0xFF55FFFF),
        37 to Color(0xFFFFFFFF),
        90 to Color(0xFF777777),
        91 to Color(0xFFFF7777),
        92 to Color(0xFF77FF77),
        93 to Color(0xFFFFFF77),
        94 to Color(0xFF7777FF),
        95 to Color(0xFFFF77FF),
        96 to Color(0xFF77FFFF),
        97 to Color(0xFFFFFFFF),
    )

    private val bgColors = mapOf(
        40 to Color(0xFF000000),
        41 to Color(0xFFFF5555),
        42 to Color(0xFF55FF55),
        43 to Color(0xFFFFFF55),
        44 to Color(0xFF5555FF),
        45 to Color(0xFFFF55FF),
        46 to Color(0xFF55FFFF),
        47 to Color(0xFFFFFFFF),
    )

    // ESC sequence regex: ESC [ ... m  or  ESC [ ... H/J/K/etc
    private val escapeRegex = Regex("\\[([0-9;]*)([A-Za-z])")

    fun parse(raw: String): AnnotatedString {
        // Strip clear screen sequences and handle cursor movement by stripping them
        val cleaned = raw
            .replace("[2J", "")
            .replace("[H", "")
            .replace("[3J", "")

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
