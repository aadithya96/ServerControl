package com.servercontrol.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.servercontrol.domain.model.LogEntry
import com.servercontrol.domain.model.LogLevel

object LogParser {

    // Matches: "2024-01-15T10:30:00+0000 hostname unit[pid]: message"
    // or journal short-iso: "2024-01-15T10:30:00+0000 hostname sshd[1234]: ..."
    private val journalPattern = Regex(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4})\\s+(\\S+)\\s+(\\S+):\\s+(.*)"
    )
    // Fallback syslog pattern: "Jan 15 10:30:00 hostname sshd[1234]: message"
    private val syslogPattern = Regex(
        "^([A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+(\\S+):\\s+(.*)"
    )

    fun parse(rawLines: List<String>): List<LogEntry> {
        return rawLines.filter { it.isNotBlank() }.map { line ->
            val journalMatch = journalPattern.find(line)
            if (journalMatch != null) {
                val (timestamp, _, source, message) = journalMatch.destructured
                LogEntry(
                    timestamp = timestamp,
                    level = detectLevel(message),
                    source = source,
                    message = message,
                    rawLine = line
                )
            } else {
                val syslogMatch = syslogPattern.find(line)
                if (syslogMatch != null) {
                    val (timestamp, _, source, message) = syslogMatch.destructured
                    LogEntry(
                        timestamp = timestamp,
                        level = detectLevel(message),
                        source = source,
                        message = message,
                        rawLine = line
                    )
                } else {
                    LogEntry(
                        timestamp = "",
                        level = detectLevel(line),
                        source = "",
                        message = line,
                        rawLine = line
                    )
                }
            }
        }
    }

    fun detectLevel(line: String): LogLevel {
        val upper = line.uppercase()
        return when {
            upper.contains("EMERG") || upper.contains("ALERT") || upper.contains("CRIT") ||
                upper.contains("ERROR") || upper.contains(" ERR ") || upper.contains("[ERR]") ||
                upper.contains("FATAL") || upper.contains("FAILED") -> LogLevel.ERROR
            upper.contains("WARN") || upper.contains("WARNING") -> LogLevel.WARN
            upper.contains("NOTICE") || upper.contains("INFO") || upper.contains("[OK]") -> LogLevel.INFO
            upper.contains("DEBUG") || upper.contains("TRACE") || upper.contains("VERBOSE") -> LogLevel.DEBUG
            else -> LogLevel.UNKNOWN
        }
    }

    fun highlight(entry: LogEntry, query: String): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(entry.message)
        return buildAnnotatedString {
            val text = entry.message
            append(text)
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            var start = lowerText.indexOf(lowerQuery)
            while (start >= 0) {
                addStyle(
                    SpanStyle(background = Color(0xFFFFEB3B), color = Color.Black),
                    start,
                    start + query.length
                )
                start = lowerText.indexOf(lowerQuery, start + 1)
            }
        }
    }
}
