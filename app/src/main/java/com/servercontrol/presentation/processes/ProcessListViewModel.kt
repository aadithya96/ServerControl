package com.servercontrol.presentation.processes

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.update
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
    private val killProcessUseCase: KillProcessUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: -1L

    private val _allProcesses = MutableStateFlow<Resource<List<Process>>>(Resource.Loading)
    val sortBy = MutableStateFlow(ProcessSortOrder.CPU)
    val searchQuery = MutableStateFlow("")
    private val _killResult = MutableStateFlow<String?>(null)
    private val _autoRefresh = MutableStateFlow(true)

    val uiState: StateFlow<ProcessUiState> = combine(
        _allProcesses, sortBy, searchQuery, _killResult, _autoRefresh
    ) { allRes, sort, query, killResult, autoRefresh ->
        val filteredSorted = when (allRes) {
            is Resource.Success -> {
                val filtered = allRes.data.filter {
                    query.isEmpty() ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.command.contains(query, ignoreCase = true)
                }
                val sorted = when (sort) {
                    ProcessSortOrder.CPU -> filtered.sortedByDescending { it.cpuPercent }
                    ProcessSortOrder.MEMORY -> filtered.sortedByDescending { it.memPercent }
                    ProcessSortOrder.PID -> filtered.sortedBy { it.pid }
                    ProcessSortOrder.NAME -> filtered.sortedBy { it.name }
                }
                ProcessUiState(
                    processes = sorted,
                    isLoading = false,
                    sortOrder = sort,
                    searchQuery = query,
                    killResult = killResult,
                    autoRefresh = autoRefresh
                )
            }
            is Resource.Error -> ProcessUiState(
                error = allRes.message,
                isLoading = false,
                sortOrder = sort,
                searchQuery = query,
                killResult = killResult,
                autoRefresh = autoRefresh
            )
            is Resource.Loading -> ProcessUiState(
                isLoading = true,
                sortOrder = sort,
                searchQuery = query,
                killResult = killResult,
                autoRefresh = autoRefresh
            )
        }
        filteredSorted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProcessUiState())

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun setSortBy(sort: ProcessSortOrder) { sortBy.value = sort }
    fun setSearchQuery(q: String) { searchQuery.value = q }
    fun setSortOrder(order: ProcessSortOrder) { sortBy.value = order }
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

    fun clearKillResult() { _killResult.value = null }

    // Legacy init for backward compat
    fun init(id: Long) {
        restartPolling()
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

    private fun restartPolling() {
        if (_autoRefresh.value) startAutoRefresh()
    }

    private suspend fun fetchProcesses() {
        val effectiveId = if (serverId != -1L) serverId else return
        when (val result = getProcessesUseCase(effectiveId, sortBy.value)) {
            is Resource.Success -> _allProcesses.value = result
            is Resource.Error -> _allProcesses.value = result
            is Resource.Loading -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
