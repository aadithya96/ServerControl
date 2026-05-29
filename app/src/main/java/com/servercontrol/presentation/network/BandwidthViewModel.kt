package com.servercontrol.presentation.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.data.remote.agent.AgentDataSource
import com.servercontrol.data.remote.dto.toDomain
import com.servercontrol.domain.model.BandwidthInfo
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BandwidthUiState(
    val interfaces: List<BandwidthInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BandwidthViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val agentDataSource: AgentDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(BandwidthUiState())
    val uiState: StateFlow<BandwidthUiState> = _uiState

    // Map of interface name -> list of (rx, tx) samples (last 30)
    private val _history = MutableStateFlow<Map<String, List<Pair<Long, Long>>>>(emptyMap())
    val history: StateFlow<Map<String, List<Pair<Long, Long>>>> = _history

    private var serverId: Long = -1
    private var pollJob: Job? = null

    fun init(serverId: Long) {
        if (this.serverId == serverId) return
        this.serverId = serverId
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                fetchBandwidth()
                delay(2000L)
            }
        }
    }

    private suspend fun fetchBandwidth() {
        val server = serverRepository.getServerById(serverId) ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)

        when (val result = agentDataSource.getBandwidth(server)) {
            is Resource.Success -> {
                val interfaces = result.data.interfaces.map { it.toDomain() }
                // Update history
                val newHistory = _history.value.toMutableMap()
                for (iface in interfaces) {
                    val existing = newHistory[iface.interfaceName] ?: emptyList()
                    val updated = (existing + (iface.rxBytesPerSec to iface.txBytesPerSec)).takeLast(30)
                    newHistory[iface.interfaceName] = updated
                }
                _history.value = newHistory
                _uiState.value = BandwidthUiState(interfaces = interfaces, isLoading = false)
            }
            is Resource.Error -> {
                _uiState.value = BandwidthUiState(
                    interfaces = _uiState.value.interfaces,
                    isLoading = false,
                    error = result.message
                )
            }
            is Resource.Loading -> {}
        }
    }

    fun refresh() {
        viewModelScope.launch { fetchBandwidth() }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
