package com.servercontrol.data.local.db

import androidx.room.*
import com.servercontrol.data.local.entity.SavedCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCommandDao {

    @Query("SELECT * FROM saved_commands WHERE serverId IS NULL OR serverId = :serverId ORDER BY isBuiltIn DESC, name ASC")
    fun getCommandsForServer(serverId: String): Flow<List<SavedCommandEntity>>

    @Query("SELECT * FROM saved_commands ORDER BY isBuiltIn DESC, name ASC")
    fun getAllCommands(): Flow<List<SavedCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(command: SavedCommandEntity)

    @Delete
    suspend fun delete(command: SavedCommandEntity)

    @Query("DELETE FROM saved_commands WHERE isBuiltIn = 1")
    suspend fun deleteAllBuiltIns()

    @Query("SELECT COUNT(*) FROM saved_commands WHERE isBuiltIn = 1")
    suspend fun countBuiltIns(): Int
}
