package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.DiskInfo

data class MountDto(
    @SerializedName("device") val device: String = "",
    @SerializedName("mount_point") val mountPoint: String = "",
    @SerializedName("fs_type") val fsType: String = "",
    @SerializedName("total_bytes") val totalBytes: Long = 0,
    @SerializedName("used_bytes") val usedBytes: Long = 0,
    @SerializedName("free_bytes") val freeBytes: Long = 0,
    @SerializedName("usage_percent") val usagePercent: Double = 0.0,
    @SerializedName("read_bytes_per_sec") val readBytesPerSec: Long = 0,
    @SerializedName("write_bytes_per_sec") val writeBytesPerSec: Long = 0,
    @SerializedName("io_wait_percent") val ioWaitPercent: Double = 0.0
)

data class DiskResponseDto(
    @SerializedName("mounts") val mounts: List<MountDto> = emptyList()
)

fun MountDto.toDomain(): DiskInfo = DiskInfo(
    device = device,
    mountPoint = mountPoint,
    fsType = fsType,
    totalBytes = totalBytes,
    usedBytes = usedBytes,
    freeBytes = freeBytes,
    readBytesPerSec = readBytesPerSec,
    writeBytesPerSec = writeBytesPerSec,
    ioWaitPercent = ioWaitPercent
)
