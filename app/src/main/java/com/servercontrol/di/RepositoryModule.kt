package com.servercontrol.di

import com.servercontrol.data.repository.ServerRepositoryImpl
import com.servercontrol.data.repository.StatsRepositoryImpl
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.domain.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository
}
