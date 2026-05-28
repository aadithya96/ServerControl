package com.servercontrol.domain.model

data class DiskInfo(
    val mountPoint: String,
    val device: String,
    val fsType: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val readBytesPerSec: Long = 0,
    val writeBytesPerSec: Long = 0,
    val ioWaitPercent: Double = 0.0
) {
    val usedPercent: Double
        get() = if (totalBytes > 0) usedBytes.toDouble() / totalBytes * 100 else 0.0
}
