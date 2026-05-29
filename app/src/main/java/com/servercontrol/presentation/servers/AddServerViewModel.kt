package com.servercontrol.presentation.servers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val editServerId: Long? = savedStateHandle.get<Long>("serverId")?.takeIf { it != -1L }

    var displayName by mutableStateOf("")
    var host by mutableStateOf("")
    var agentPort by mutableStateOf("9876")
    var authType by mutableStateOf(AuthType.AGENT_TOKEN)
    var agentToken by mutableStateOf("")
    var sshUser by mutableStateOf("")
    var sshPassword by mutableStateOf("")
    var sshKeyPath by mutableStateOf("")
    var sshPort by mutableStateOf("22")

    val testConnectionState: MutableStateFlow<Resource<Long>?> = MutableStateFlow(null)
    val saveState: MutableStateFlow<Resource<Unit>?> = MutableStateFlow(null)

    val isFormValid: StateFlow<Boolean> = combine(
        snapshotFlow { displayName },
        snapshotFlow { host },
        snapshotFlow { authType },
        snapshotFlow { agentToken },
        snapshotFlow { sshUser }
    ) { name, h, auth, token, user ->
        if (name.isBlank() || h.isBlank()) return@combine false
        when (auth) {
            AuthType.AGENT_TOKEN -> token.isNotBlank()
            AuthType.SSH_PASSWORD -> user.isNotBlank()
            AuthType.SSH_KEY -> user.isNotBlank()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        editServerId?.let { loadServer(it) }
    }

    fun loadServer(id: Long) {
        viewModelScope.launch {
            val server = serverRepository.getServerById(id) ?: return@launch
            displayName = server.name
            host = server.host
            agentPort = server.agentPort.toString()
            sshPort = server.sshPort.toString()
            authType = server.authType
            agentToken = server.agentToken ?: ""
            sshUser = server.sshUsername ?: ""
            sshPassword = server.sshPassword ?: ""
            sshKeyPath = server.sshPrivateKey ?: ""
        }
    }

    fun testConnection() {
        val profile = buildProfile() ?: run {
            testConnectionState.value = Resource.Error("Please fill all required fields")
            return
        }
        testConnectionState.value = Resource.Loading
        viewModelScope.launch {
            val result = serverRepository.testConnection(profile)
            testConnectionState.value = result.fold(
                onSuccess = { Resource.Success(it) },
                onFailure = { Resource.Error(it.message ?: "Connection failed") }
            )
        }
    }

    fun saveServer() {
        val profile = buildProfile() ?: run {
            saveState.value = Resource.Error("Please fill all required fields")
            return
        }
        saveState.value = Resource.Loading
        viewModelScope.launch {
            try {
                if (editServerId != null) {
                    serverRepository.updateServer(profile.copy(id = editServerId))
                } else {
                    serverRepository.insertServer(profile)
                }
                saveState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                saveState.value = Resource.Error(e.message ?: "Save failed")
            }
        }
    }

    private fun buildProfile(): ServerProfile? {
        if (displayName.isBlank() || host.isBlank()) return null
        return ServerProfile(
            id = editServerId ?: 0L,
            name = displayName.trim(),
            host = host.trim(),
            agentPort = agentPort.toIntOrNull() ?: 9876,
            sshPort = sshPort.toIntOrNull() ?: 22,
            authType = authType,
            agentToken = agentToken.takeIf { it.isNotBlank() },
            sshUsername = sshUser.takeIf { it.isNotBlank() },
            sshPassword = sshPassword.takeIf { it.isNotBlank() },
            sshPrivateKey = sshKeyPath.takeIf { it.isNotBlank() }
        )
    }
}
