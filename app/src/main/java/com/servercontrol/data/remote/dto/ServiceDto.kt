package com.servercontrol.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.servercontrol.domain.model.SystemService

data class ServiceDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("load_state") val loadState: String = "",
    @SerializedName("active_state") val activeState: String = "",
    @SerializedName("sub_state") val subState: String = "",
    @SerializedName("enabled") val enabled: String = "",
    @SerializedName("unit_file") val unitFile: String = "",
    @SerializedName("exec_start") val execStart: String = "",
    @SerializedName("type") val type: String = ""
)

data class ServicesResponseDto(
    @SerializedName("services") val services: List<ServiceDto> = emptyList(),
    @SerializedName("total") val total: Int = 0
)

data class ServiceActionRequest(
    @SerializedName("action") val action: String
)

data class ServiceActionResponseDto(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)

data class ServiceLogsResponseDto(
    @SerializedName("lines") val lines: List<String> = emptyList()
)

fun ServiceDto.toDomain() = SystemService(
    name = name,
    description = description,
    loadState = loadState,
    activeState = activeState,
    subState = subState,
    enabled = enabled == "enabled",
    unitFilePath = unitFile,
    execStart = execStart,
    type = type
)
