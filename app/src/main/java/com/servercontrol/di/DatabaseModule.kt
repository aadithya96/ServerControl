package com.servercontrol.di

import android.content.Context
import androidx.room.Room
import com.servercontrol.data.local.db.AppDatabase
import com.servercontrol.data.local.db.AuditLogDao
import com.servercontrol.data.local.db.MetricSampleDao
import com.servercontrol.data.local.db.SavedCommandDao
import com.servercontrol.data.local.db.ServerProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideServerProfileDao(db: AppDatabase): ServerProfileDao = db.serverProfileDao()

    @Provides
    @Singleton
    fun provideMetricSampleDao(db: AppDatabase): MetricSampleDao = db.metricSampleDao()

    @Provides
    @Singleton
    fun provideSavedCommandDao(db: AppDatabase): SavedCommandDao = db.savedCommandDao()

    @Provides
    @Singleton
    fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()
}
