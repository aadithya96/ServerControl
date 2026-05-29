package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExecRequest(
    @SerializedName("command") val command: String,
    @SerializedName("timeout_seconds") val timeoutSeconds: Int = 30
)

data class ExecResponseDto(
    @SerializedName("output") val output: String = "",
    @SerializedName("exit_code") val exitCode: Int = 0,
    @SerializedName("success") val success: Boolean = false
)
