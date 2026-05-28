package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName

data class KillResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)

data class FirewallToggleRequest(
    @SerializedName("rule_id") val ruleId: String,
    @SerializedName("enabled") val enabled: Boolean
)

data class FirewallToggleResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)

data class HealthDto(
    @SerializedName("status") val status: String = "",
    @SerializedName("version") val version: String = ""
)
