package com.servercontrol.domain.model

data class BandwidthInfo(
    val interfaceName: String,
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long,
    val rxTotalBytes: Long,
    val txTotalBytes: Long
) {
    fun rxMbps(): Float = rxBytesPerSec / 1_048_576f
    fun txMbps(): Float = txBytesPerSec / 1_048_576f

    fun formatRx(): String = formatBytes(rxBytesPerSec)
    fun formatTx(): String = formatBytes(txBytesPerSec)

    private fun formatBytes(bps: Long): String = when {
        bps >= 1_073_741_824L -> "${"%.1f".format(bps / 1_073_741_824f)} GB/s"
        bps >= 1_048_576L -> "${"%.1f".format(bps / 1_048_576f)} MB/s"
        bps >= 1024L -> "${"%.1f".format(bps / 1024f)} KB/s"
        else -> "$bps B/s"
    }
}
