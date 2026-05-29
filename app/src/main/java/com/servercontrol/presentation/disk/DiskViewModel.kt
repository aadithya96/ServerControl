package com.servercontrol.presentation.disk

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.domain.usecase.GetDiskInfoUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiskUiState(
    val disks: List<DiskInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DiskViewModel @Inject constructor(
    private val getDiskInfoUseCase: GetDiskInfoUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: -1L

    private val _uiState = MutableStateFlow(DiskUiState())
    val uiState: StateFlow<DiskUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadDisk()
    }

    fun refresh() {
        loadDisk()
    }

    // Legacy init for backward compat
    fun init(id: Long) {
        restartPolling()
    }

    private fun loadDisk() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                fetchDisk()
                delay(15_000L)
            }
        }
    }

    private fun restartPolling() {
        loadDisk()
    }

    private suspend fun fetchDisk() {
        val effectiveId = if (serverId != -1L) serverId else return
        _uiState.value = _uiState.value.copy(isLoading = true)
        when (val result = getDiskInfoUseCase(effectiveId)) {
            is Resource.Success -> _uiState.value = DiskUiState(disks = result.data, isLoading = false)
            is Resource.Error -> _uiState.value = DiskUiState(
                disks = _uiState.value.disks,
                isLoading = false,
                error = result.message
            )
            is Resource.Loading -> Unit
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
