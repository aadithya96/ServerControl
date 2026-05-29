package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.BandwidthInfo

data class BandwidthResponseDto(
    @SerializedName("interfaces") val interfaces: List<BandwidthInterfaceDto> = emptyList()
)

data class BandwidthInterfaceDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("rx_bps") val rxBps: Long = 0,
    @SerializedName("tx_bps") val txBps: Long = 0,
    @SerializedName("rx_total") val rxTotal: Long = 0,
    @SerializedName("tx_total") val txTotal: Long = 0
)

fun BandwidthInterfaceDto.toDomain() = BandwidthInfo(
    interfaceName = name,
    rxBytesPerSec = rxBps,
    txBytesPerSec = txBps,
    rxTotalBytes = rxTotal,
    txTotalBytes = txTotal
)
