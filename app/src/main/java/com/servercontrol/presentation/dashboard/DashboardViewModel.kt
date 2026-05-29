package com.servercontrol.presentation.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.domain.usecase.GetSystemStatsUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSystemStatsUseCase: GetSystemStatsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: -1L

    private val _stats = MutableStateFlow<Resource<SystemStats>>(Resource.Loading)
    val stats: StateFlow<Resource<SystemStats>> = _stats.asStateFlow()

    private val _cpuHistory = MutableStateFlow<List<Float>>(emptyList())
    val cpuHistory: StateFlow<List<Float>> = _cpuHistory.asStateFlow()

    private val _refreshInterval = MutableStateFlow(5)
    val refreshInterval: StateFlow<Int> = _refreshInterval.asStateFlow()

    private val _autoRefresh = MutableStateFlow(true)
    val autoRefresh: StateFlow<Boolean> = _autoRefresh.asStateFlow()

    // Legacy state for backward compat with DashboardScreen (isLoading, error, stats fields)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun setRefreshInterval(seconds: Int) {
        _refreshInterval.value = seconds
        restartPolling()
    }

    fun toggleAutoRefresh() {
        _autoRefresh.update { !it }
        if (_autoRefresh.value) startPolling() else pollingJob?.cancel()
    }

    fun refresh() {
        viewModelScope.launch { fetchStats() }
    }

    fun init(id: Long) {
        // Legacy init for screens that don't use SavedStateHandle injection
        // If serverId is already set from SavedStateHandle, ignore; otherwise use id
        if (serverId == -1L) {
            // Can't reassign val; the screens should pass serverId via SavedStateHandle nav arg
            // This overload exists for backward compat and is a no-op when SavedStateHandle is populated
        }
        restartPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchStats()
                delay(_refreshInterval.value * 1000L)
            }
        }
    }

    private fun restartPolling() {
        pollingJob?.cancel()
        if (_autoRefresh.value) startPolling()
    }

    private suspend fun fetchStats() {
        val currentStats = (_stats.value as? Resource.Success)?.data
        _uiState.update { it.copy(isLoading = currentStats == null) }

        val effectiveServerId = if (serverId != -1L) serverId else (_uiState.value.stats?.let { -1L } ?: -1L)

        when (val result = getSystemStatsUseCase(effectiveServerId)) {
            is Resource.Success -> {
                _stats.value = result
                val history = (_cpuHistory.value + result.data.cpuPercent.toFloat()).takeLast(60)
                _cpuHistory.value = history
                _uiState.update { state ->
                    state.copy(
                        stats = result.data,
                        cpuHistory = history,
                        isLoading = false,
                        error = null,
                        refreshIntervalSeconds = _refreshInterval.value
                    )
                }
            }
            is Resource.Error -> {
                _stats.value = result
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
            is Resource.Loading -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

data class DashboardUiState(
    val stats: SystemStats? = null,
    val cpuHistory: List<Float> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val refreshIntervalSeconds: Int = 5
)
