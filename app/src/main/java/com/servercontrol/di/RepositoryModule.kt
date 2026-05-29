package com.servercontrol.di

import com.servercontrol.data.repository.CommandRepositoryImpl
import com.servercontrol.data.repository.DockerRepositoryImpl
import com.servercontrol.data.repository.MetricsRepositoryImpl
import com.servercontrol.data.repository.SecurityRepositoryImpl
import com.servercontrol.data.repository.ServerRepositoryImpl
import com.servercontrol.data.repository.StatsRepositoryImpl
import com.servercontrol.domain.repository.CommandRepository
import com.servercontrol.domain.repository.DockerRepository
import com.servercontrol.domain.repository.MetricsRepository
import com.servercontrol.domain.repository.SecurityRepository
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

    @Binds
    @Singleton
    abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository

    @Binds
    @Singleton
    abstract fun bindDockerRepository(impl: DockerRepositoryImpl): DockerRepository

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository

    @Binds
    @Singleton
    abstract fun bindCommandRepository(impl: CommandRepositoryImpl): CommandRepository
}
