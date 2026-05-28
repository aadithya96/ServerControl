package com.servercontrol.presentation.processes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.ProcessSortOrder
import com.servercontrol.domain.usecase.GetProcessesUseCase
import com.servercontrol.domain.usecase.KillProcessUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessUiState(
    val processes: List<Process> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortOrder: ProcessSortOrder = ProcessSortOrder.CPU,
    val searchQuery: String = "",
    val killResult: String? = null,
    val autoRefresh: Boolean = true
)

@HiltViewModel
class ProcessListViewModel @Inject constructor(
    private val getProcessesUseCase: GetProcessesUseCase,
    private val killProcessUseCase: KillProcessUseCase
) : ViewModel() {

    private val _processes = MutableStateFlow<List<Process>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _sortOrder = MutableStateFlow(ProcessSortOrder.CPU)
    private val _searchQuery = MutableStateFlow("")
    private val _killResult = MutableStateFlow<String?>(null)
    private val _autoRefresh = MutableStateFlow(true)

    val uiState: StateFlow<ProcessUiState> = combine(
        _processes, _isLoading, _error, _sortOrder, _searchQuery, _killResult, _autoRefresh
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ProcessUiState(
            processes = values[0] as List<Process>,
            isLoading = values[1] as Boolean,
            error = values[2] as String?,
            sortOrder = values[3] as ProcessSortOrder,
            searchQuery = values[4] as String,
            killResult = values[5] as String?,
            autoRefresh = values[6] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProcessUiState())

    private var refreshJob: Job? = null
    private var serverId: Long = -1L

    fun init(serverId: Long) {
        this.serverId = serverId
        startAutoRefresh()
    }

    fun setSortOrder(order: ProcessSortOrder) { _sortOrder.value = order }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setAutoRefresh(enabled: Boolean) {
        _autoRefresh.value = enabled
        if (enabled) startAutoRefresh() else refreshJob?.cancel()
    }

    fun refresh() {
        viewModelScope.launch { fetchProcesses() }
    }

    fun killProcess(pid: Int) {
        viewModelScope.launch {
            when (val result = killProcessUseCase(serverId, pid)) {
                is Resource.Success -> {
                    _killResult.value = "Process $pid killed"
                    fetchProcesses()
                }
                is Resource.Error -> _killResult.value = "Failed: ${result.message}"
                is Resource.Loading -> Unit
            }
            delay(3000)
            _killResult.value = null
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                fetchProcesses()
                delay(5000)
            }
        }
    }

    private suspend fun fetchProcesses() {
        _isLoading.value = _processes.value.isEmpty()
        when (val result = getProcessesUseCase(serverId, _sortOrder.value)) {
            is Resource.Success -> {
                _processes.value = result.data
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

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
