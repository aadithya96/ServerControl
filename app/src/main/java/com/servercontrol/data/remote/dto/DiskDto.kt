package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.DiskInfo

data class DiskDto(
    @SerializedName("mount_point") val mountPoint: String,
    @SerializedName("device") val device: String,
    @SerializedName("fs_type") val fsType: String,
    @SerializedName("total_bytes") val totalBytes: Long,
    @SerializedName("used_bytes") val usedBytes: Long,
    @SerializedName("free_bytes") val freeBytes: Long,
    @SerializedName("read_bps") val readBytesPerSec: Long = 0,
    @SerializedName("write_bps") val writeBytesPerSec: Long = 0,
    @SerializedName("io_wait_percent") val ioWaitPercent: Double = 0.0
)

fun DiskDto.toDomain(): DiskInfo = DiskInfo(
    mountPoint = mountPoint,
    device = device,
    fsType = fsType,
    totalBytes = totalBytes,
    usedBytes = usedBytes,
    freeBytes = freeBytes,
    readBytesPerSec = readBytesPerSec,
    writeBytesPerSec = writeBytesPerSec,
    ioWaitPercent = ioWaitPercent
)
