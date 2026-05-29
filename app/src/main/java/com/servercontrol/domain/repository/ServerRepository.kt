package com.servercontrol.domain.repository

import com.servercontrol.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getServers(): Flow<List<ServerProfile>>
    suspend fun getServerById(id: Long): ServerProfile?
    suspend fun insertServer(server: ServerProfile): Long
    suspend fun updateServer(server: ServerProfile)
    suspend fun deleteServer(server: ServerProfile)
    suspend fun testConnection(server: ServerProfile): Result<Long>
    suspend fun getAllServers(): List<ServerProfile>
}
