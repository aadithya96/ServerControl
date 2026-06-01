package com.servercontrol.util

/**
 * Centralised, consistent formatting helpers.
 *
 * Historically `formatBytes()` was copy-pasted into several screens with subtly
 * different behaviour at boundary values. New code should use these helpers
 * instead of defining a local copy.
 */
object FormatUtils {

    /**
     * Formats a raw byte count into a human-readable string using binary (1024)
     * units, e.g. `1536` -> `"1.5 KB"`, `0` -> `"0 B"`.
     */
    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        val tb = gb / 1024.0
        return when {
            tb >= 1.0 -> "%.2f TB".format(tb)
            gb >= 1.0 -> "%.1f GB".format(gb)
            mb >= 1.0 -> "%.1f MB".format(mb)
            kb >= 1.0 -> "%.1f KB".format(kb)
            else -> "$bytes B"
        }
    }

    /**
     * Formats a bytes-per-second rate, e.g. `"1.5 MB/s"`.
     */
    fun formatBytesPerSec(bytesPerSec: Long): String = "${formatBytes(bytesPerSec)}/s"
}
