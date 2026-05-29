package com.servercontrol.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.servercontrol.data.local.db.SavedCommandDao
import com.servercontrol.data.local.entity.SavedCommandEntity
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.local.entity.toEntity
import com.servercontrol.domain.model.SavedCommand
import com.servercontrol.domain.repository.CommandRepository
import com.servercontrol.util.BuiltInCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CommandRepositoryImpl @Inject constructor(
    private val savedCommandDao: SavedCommandDao
) : CommandRepository {

    override fun getCommandsForServer(serverId: String): Flow<List<SavedCommand>> =
        savedCommandDao.getCommandsForServer(serverId).map { list -> list.map { it.toDomain() } }

    override fun getAllCommands(): Flow<List<SavedCommand>> =
        savedCommandDao.getAllCommands().map { list -> list.map { it.toDomain() } }

    override suspend fun saveCommand(command: SavedCommand) {
        savedCommandDao.upsert(command.toEntity())
    }

    override suspend fun deleteCommand(id: String) {
        savedCommandDao.delete(SavedCommandEntity(id = id, serverId = null, name = "", command = "", description = ""))
    }

    override suspend fun seedBuiltIns() {
        val count = savedCommandDao.countBuiltIns()
        if (count == 0) {
            BuiltInCommands.all.forEach { savedCommandDao.upsert(it.toEntity()) }
        }
    }

    override suspend fun exportJson(): String {
        var result = ""
        getAllCommands().collect { commands ->
            result = Gson().toJson(commands.filter { !it.isBuiltIn })
            return@collect
        }
        return result
    }

    override suspend fun importJson(json: String) {
        val type = object : TypeToken<List<SavedCommand>>() {}.type
        val commands: List<SavedCommand> = Gson().fromJson(json, type) ?: return
        commands.forEach { cmd ->
            savedCommandDao.upsert(cmd.copy(isBuiltIn = false).toEntity())
        }
    }
}
