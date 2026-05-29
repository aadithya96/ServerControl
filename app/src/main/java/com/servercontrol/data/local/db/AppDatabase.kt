package com.servercontrol.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.servercontrol.data.local.entity.AuditLogEntity
import com.servercontrol.data.local.entity.MetricSampleEntity
import com.servercontrol.data.local.entity.SavedCommandEntity
import com.servercontrol.data.local.entity.ServerProfileEntity

@Database(
    entities = [
        ServerProfileEntity::class,
        MetricSampleEntity::class,
        SavedCommandEntity::class,
        AuditLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverProfileDao(): ServerProfileDao
    abstract fun metricSampleDao(): MetricSampleDao
    abstract fun savedCommandDao(): SavedCommandDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        const val DATABASE_NAME = "server_control.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `metric_samples` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `serverId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `cpuPercent` REAL NOT NULL,
                        `memUsedBytes` INTEGER NOT NULL,
                        `memTotalBytes` INTEGER NOT NULL,
                        `diskUsedBytes` INTEGER NOT NULL,
                        `diskTotalBytes` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS saved_commands (
                        id TEXT NOT NULL PRIMARY KEY,
                        serverId TEXT,
                        name TEXT NOT NULL,
                        command TEXT NOT NULL,
                        description TEXT NOT NULL,
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS audit_log (
                        id TEXT NOT NULL PRIMARY KEY,
                        timestamp INTEGER NOT NULL,
                        serverId TEXT NOT NULL,
                        serverName TEXT NOT NULL,
                        action TEXT NOT NULL,
                        details TEXT NOT NULL,
                        result TEXT NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE server_profiles ADD COLUMN groupName TEXT NOT NULL DEFAULT 'default'"
                )
            }
        }
    }
}
