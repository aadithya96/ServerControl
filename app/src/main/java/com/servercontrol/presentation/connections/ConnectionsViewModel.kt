package com.servercontrol.presentation.connections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.domain.usecase.GetConnectionsUseCase
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
class ConnectionsViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle.get<Long>("serverId") ?: -1L

    private val _allConnections = MutableStateFlow<Resource<List<NetworkConnection>>>(Resource.Loading)
    val protoFilter = MutableStateFlow("all")
    val stateFilter = MutableStateFlow("all")
    val searchQuery = MutableStateFlow("")

    val connections: StateFlow<Resource<List<NetworkConnection>>> = combine(
        _allConnections, protoFilter, stateFilter, searchQuery
    ) { resource, proto, state, query ->
        when (resource) {
            is Resource.Loading -> resource
            is Resource.Error -> resource
            is Resource.Success -> {
                val filtered = resource.data.filter { conn ->
                    (proto == "all" || conn.protocol.equals(proto, ignoreCase = true)) &&
                    (state == "all" || conn.state.equals(state, ignoreCase = true)) &&
                    (query.isEmpty() ||
                        conn.localAddress.contains(query, ignoreCase = true) ||
                        conn.remoteAddress.contains(query, ignoreCase = true) ||
                        (conn.processName?.contains(query, ignoreCase = true) == true))
                }
                Resource.Success(filtered)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading)

    private var autoRefreshJob: Job? = null

    init {
        loadConnections()
    }

    fun setProtoFilter(proto: String) { protoFilter.value = proto }
    fun setStateFilter(state: String) { stateFilter.value = state }
    fun setSearchQuery(q: String) { searchQuery.value = q }

    fun refresh() {
        autoRefreshJob?.cancel()
        loadConnections()
    }

    private fun loadConnections() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                _allConnections.value = Resource.Loading
                _allConnections.value = getConnectionsUseCase(serverId)
                delay(10_000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
