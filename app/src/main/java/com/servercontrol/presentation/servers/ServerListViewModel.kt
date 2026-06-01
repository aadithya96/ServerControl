package com.servercontrol.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.domain.usecase.GetServersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerListViewModel @Inject constructor(
    getServersUseCase: GetServersUseCase,
    private val serverRepository: ServerRepository
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
