package com.servercontrol.di

import com.servercontrol.data.remote.WebhookService
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.terminal.TerminalManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideWebhookService(okHttpClient: OkHttpClient): WebhookService = WebhookService(okHttpClient)

    @Provides
    @Singleton
    fun provideAgentDataSource(): AgentDataSource = AgentDataSource()

    @Provides
    @Singleton
    fun provideSshDataSource(): SshDataSource = SshDataSource()

    @Provides
    @Singleton
    fun provideTerminalManager(): TerminalManager = TerminalManager()
}
