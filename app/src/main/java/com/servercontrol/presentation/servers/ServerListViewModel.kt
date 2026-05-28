package com.servercontrol.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.domain.usecase.GetServersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun deleteServer(server: ServerProfile) {
        viewModelScope.launch {
            serverRepository.deleteServer(server)
        }
    }
}
