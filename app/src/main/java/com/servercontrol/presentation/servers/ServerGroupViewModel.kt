package com.servercontrol.presentation.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerGroupViewModel @Inject constructor(
    private val serverRepository: ServerRepository
) : ViewModel() {

    val groups = serverRepository.getDistinctGroups()

    fun setGroup(serverId: Long, group: String) {
        viewModelScope.launch {
            serverRepository.setServerGroup(serverId, group)
        }
    }
}
