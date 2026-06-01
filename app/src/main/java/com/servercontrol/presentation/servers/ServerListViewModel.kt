package com.servercontrol.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.domain.usecase.GetDiskInfoUseCase
import com.servercontrol.domain.usecase.GetServersUseCase
import com.servercontrol.domain.usecase.GetSystemStatsUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerMetrics(val cpu: Float, val mem: Float, val disk: Float)

@HiltViewModel
class ServerListViewModel @Inject constructor(
    getServersUseCase: GetServersUseCase,
    private val serverRepository: ServerRepository,
    private val getSystemStatsUseCase: GetSystemStatsUseCase,
    private val getDiskInfoUseCase: GetDiskInfoUseCase
) : ViewModel() {

    val servers: StateFlow<List<ServerProfile>> = getServersUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedServerId: MutableStateFlow<Long?> = MutableStateFlow(null)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _serverMetrics = MutableStateFlow<Map<Long, ServerMetrics>>(emptyMap())
    val serverMetrics: StateFlow<Map<Long, ServerMetrics>> = _serverMetrics.asStateFlow()

    init {
        refreshStatuses()
    }

    fun refreshStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                serverRepository.getAllServers().map { server ->
                    async {
                        val online = serverRepository.testConnection(server).isSuccess
                        serverRepository.updateServer(server.copy(isOnline = online))
                        if (online) {
                            val stats = getSystemStatsUseCase(server.id)
                            val disk = getDiskInfoUseCase(server.id)
                            val cpu = (stats as? Resource.Success)?.data?.cpuPercent?.toFloat() ?: 0f
                            val mem = (stats as? Resource.Success)?.data?.memPercent?.toFloat() ?: 0f
                            val diskPct = (disk as? Resource.Success)?.data
                                ?.filter { it.totalBytes > 0 }
                                ?.maxByOrNull { it.totalBytes }
                                ?.usedPercent?.toFloat() ?: 0f
                            _serverMetrics.update { it + (server.id to ServerMetrics(cpu, mem, diskPct)) }
                        }
                    }
                }.awaitAll()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            val server = serverRepository.getServerById(id) ?: return@launch
            serverRepository.deleteServer(server)
        }
    }

    fun deleteServer(server: ServerProfile) {
        viewModelScope.launch {
            serverRepository.deleteServer(server)
        }
    }

    fun selectServer(id: Long) {
        selectedServerId.value = id
    }

    fun importServer(profile: ServerProfile) {
        viewModelScope.launch {
            // Import with id=0 so Room assigns a new ID
            serverRepository.insertServer(profile.copy(id = 0))
        }
    }
}
