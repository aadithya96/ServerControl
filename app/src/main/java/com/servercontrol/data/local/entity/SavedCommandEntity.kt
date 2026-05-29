package com.servercontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.servercontrol.domain.model.SavedCommand
import java.util.UUID

@Entity(tableName = "saved_commands")
data class SavedCommandEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val serverId: String?,
    val name: String,
    val command: String,
    val description: String,
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun SavedCommandEntity.toDomain() = SavedCommand(
    id = id,
    serverId = serverId,
    name = name,
    command = command,
    description = description,
    isBuiltIn = isBuiltIn
)

fun SavedCommand.toEntity() = SavedCommandEntity(
    id = id,
    serverId = serverId,
    name = name,
    command = command,
    description = description,
    isBuiltIn = isBuiltIn
)
