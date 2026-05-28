package com.servercontrol.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.servercontrol.data.local.entity.ServerProfileEntity

@Database(
    entities = [ServerProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverProfileDao(): ServerProfileDao

    companion object {
        const val DATABASE_NAME = "server_control.db"
    }
}
