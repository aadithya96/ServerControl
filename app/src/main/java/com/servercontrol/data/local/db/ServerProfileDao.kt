package com.servercontrol.data.local.db

import androidx.room.*
import com.servercontrol.data.local.entity.ServerProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerProfileDao {

    @Query("SELECT * FROM server_profiles ORDER BY name ASC")
    fun getAllServers(): Flow<List<ServerProfileEntity>>

    @Query("SELECT * FROM server_profiles WHERE id = :id")
    suspend fun getServerById(id: Long): ServerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerProfileEntity): Long

    @Update
    suspend fun updateServer(server: ServerProfileEntity)

    @Delete
    suspend fun deleteServer(server: ServerProfileEntity)

    @Query("SELECT * FROM server_profiles ORDER BY name ASC")
    suspend fun getAllServersOnce(): List<ServerProfileEntity>

    @Query("UPDATE server_profiles SET groupName = :group WHERE id = :serverId")
    suspend fun setServerGroup(serverId: Long, group: String)

    @Query("SELECT DISTINCT groupName FROM server_profiles ORDER BY groupName ASC")
    fun getDistinctGroups(): Flow<List<String>>
}
