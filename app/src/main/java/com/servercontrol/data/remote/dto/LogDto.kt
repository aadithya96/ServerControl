package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LogResponseDto(
    @SerializedName("lines") val lines: List<String> = emptyList(),
    @SerializedName("source") val source: String = ""
)
