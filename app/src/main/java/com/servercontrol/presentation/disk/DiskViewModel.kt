package com.servercontrol.presentation.disk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.domain.usecase.GetDiskInfoUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val getDiskInfoUseCase: GetDiskInfoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiskUiState())
    val uiState: StateFlow<DiskUiState> = _uiState.asStateFlow()

    private var serverId: Long = -1L

    fun init(serverId: Long) {
        this.serverId = serverId
        load()
    }

    fun refresh() { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = getDiskInfoUseCase(serverId)) {
                is Resource.Success -> _uiState.value = DiskUiState(disks = result.data)
                is Resource.Error -> _uiState.value = DiskUiState(error = result.message)
                is Resource.Loading -> Unit
            }
        }
    }
}
