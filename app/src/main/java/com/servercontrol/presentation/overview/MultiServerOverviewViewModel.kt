package com.servercontrol.presentation.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.dto.toDomain
import com.servercontrol.data.remote.ssh.SshDataSource
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MultiServerOverviewViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val agentDataSource: AgentDataSource,
    private val sshDataSource: SshDataSource
) : ViewModel() {

    val servers: StateFlow<List<ServerProfile>> = serverRepository.getServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _serverStats = MutableStateFlow<Map<Long, Resource<SystemStats>>>(emptyMap())
    val serverStats: StateFlow<Map<Long, Resource<SystemStats>>> = _serverStats

    val groups: StateFlow<List<String>> = serverRepository.getDistinctGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroup = MutableStateFlow<String?>(null)
    val compareMode = MutableStateFlow(false)
    val compareServerIds = MutableStateFlow<List<Long>>(emptyList())

    private val pollJobs = mutableMapOf<Long, Job>()

    init {
        viewModelScope.launch {
            servers.collect { serverList ->
                // Cancel jobs for removed servers
                val currentIds = serverList.map { it.id }.toSet()
                pollJobs.keys.filter { it !in currentIds }.forEach { id ->
                    pollJobs.remove(id)?.cancel()
                }
                // Start polling for new servers
                for (server in serverList) {
                    if (server.id !in pollJobs) {
                        pollJobs[server.id] = launchPollJob(server)
                    }
                }
            }
        }
    }

    private fun launchPollJob(server: ServerProfile): Job = viewModelScope.launch {
        while (isActive) {
            fetchStats(server)
            delay(30_000L)
        }
    }

    private suspend fun fetchStats(server: ServerProfile) {
        _serverStats.value = _serverStats.value + (server.id to Resource.Loading)
        val result: Resource<SystemStats> = try {
            if (server.authType == AuthType.AGENT_TOKEN) {
                when (val r = agentDataSource.getStats(server)) {
                    is Resource.Success -> Resource.Success(r.data.toDomain())
                    is Resource.Error -> Resource.Error(r.message)
                    is Resource.Loading -> Resource.Loading
                }
            } else {
                val stats = sshDataSource.getSystemStats(server)
                if (stats.isSuccess) Resource.Success(stats.getOrThrow())
                else Resource.Error(stats.exceptionOrNull()?.message ?: "SSH error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
        _serverStats.value = _serverStats.value + (server.id to result)
    }

    fun selectGroup(group: String?) {
        selectedGroup.value = group
    }

    fun toggleCompareMode() {
        compareMode.value = !compareMode.value
        if (!compareMode.value) compareServerIds.value = emptyList()
    }

    fun toggleCompareServer(serverId: Long) {
        val current = compareServerIds.value
        compareServerIds.value = if (serverId in current) {
            current - serverId
        } else if (current.size < 2) {
            current + serverId
        } else {
            // Replace the first one when already 2 selected
            current.drop(1) + serverId
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val currentServers = servers.value
            for (server in currentServers) {
                launch { fetchStats(server) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJobs.values.forEach { it.cancel() }
        pollJobs.clear()
    }
}
