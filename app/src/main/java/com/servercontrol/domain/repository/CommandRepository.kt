package com.servercontrol.domain.repository

import com.servercontrol.domain.model.SavedCommand
import kotlinx.coroutines.flow.Flow

interface CommandRepository {
    fun getCommandsForServer(serverId: String): Flow<List<SavedCommand>>
    fun getAllCommands(): Flow<List<SavedCommand>>
    suspend fun saveCommand(command: SavedCommand)
    suspend fun deleteCommand(id: String)
    suspend fun seedBuiltIns()
    suspend fun exportJson(): String
    suspend fun importJson(json: String)
}
