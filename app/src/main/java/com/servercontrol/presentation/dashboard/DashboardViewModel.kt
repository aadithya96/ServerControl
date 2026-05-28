package com.servercontrol.presentation.dashboard

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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val stats: SystemStats? = null,
    val cpuHistory: List<Float> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val refreshIntervalSeconds: Int = 5
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSystemStatsUseCase: GetSystemStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var serverId: Long = -1L

    fun init(serverId: Long) {
        this.serverId = serverId
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch { fetchStats() }
    }

    fun setRefreshInterval(seconds: Int) {
        _uiState.value = _uiState.value.copy(refreshIntervalSeconds = seconds)
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                fetchStats()
                delay(_uiState.value.refreshIntervalSeconds * 1000L)
            }
        }
    }

    private suspend fun fetchStats() {
        _uiState.value = _uiState.value.copy(isLoading = _uiState.value.stats == null)
        when (val result = getSystemStatsUseCase(serverId)) {
            is Resource.Success -> {
                val history = (_uiState.value.cpuHistory + result.data.cpuPercent.toFloat())
                    .takeLast(60)
                _uiState.value = _uiState.value.copy(
                    stats = result.data,
                    cpuHistory = history,
                    isLoading = false,
                    error = null
                )
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
            is Resource.Loading -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
