package com.servercontrol.presentation.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.domain.usecase.GetConnectionsUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionsUiState(
    val connections: List<NetworkConnection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val protocolFilter: String = "ALL",
    val stateFilter: String = "ALL",
    val searchQuery: String = ""
)

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val getConnectionsUseCase: GetConnectionsUseCase
) : ViewModel() {

    private val _connections = MutableStateFlow<List<NetworkConnection>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _protocolFilter = MutableStateFlow("ALL")
    private val _stateFilter = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ConnectionsUiState> = combine(
        _connections, _isLoading, _error, _protocolFilter, _stateFilter, _searchQuery
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ConnectionsUiState(
            connections = values[0] as List<NetworkConnection>,
            isLoading = values[1] as Boolean,
            error = values[2] as String?,
            protocolFilter = values[3] as String,
            stateFilter = values[4] as String,
            searchQuery = values[5] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionsUiState())

    private var serverId: Long = -1L

    fun init(serverId: Long) {
        this.serverId = serverId
        load()
    }

    fun refresh() { load() }
    fun setProtocolFilter(proto: String) { _protocolFilter.value = proto }
    fun setStateFilter(state: String) { _stateFilter.value = state }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = getConnectionsUseCase(serverId)) {
                is Resource.Success -> {
                    _connections.value = result.data
                    _isLoading.value = false
                    _error.value = null
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = result.message
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
