package com.servercontrol.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResource(): Flow<Resource<T>> =
    this.map<T, Resource<T>> { Resource.Success(it) }
        .onStart { emit(Resource.Loading) }
        .catch { emit(Resource.Error(it.message ?: "Unknown error", it)) }

fun Long.toReadableUptime(): String {
    val days = this / 86400
    val hours = (this % 86400) / 3600
    val minutes = (this % 3600) / 60
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

fun Long.toReadableBytes(): String = FormatUtils.formatBytes(this)

fun Double.toPercentString(): String = "%.1f%%".format(this)
