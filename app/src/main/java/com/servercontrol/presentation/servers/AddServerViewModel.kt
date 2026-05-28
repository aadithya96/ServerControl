package com.servercontrol.presentation.servers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddServerUiState(
    val name: String = "",
    val host: String = "",
    val agentPort: String = "9876",
    val sshPort: String = "22",
    val authType: AuthType = AuthType.AGENT_TOKEN,
    val agentToken: String = "",
    val sshUsername: String = "",
    val sshPassword: String = "",
    val sshPrivateKey: String = "",
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddServerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val serverId: Long = savedStateHandle["serverId"] ?: -1L

    private val _uiState = MutableStateFlow(AddServerUiState())
    val uiState: StateFlow<AddServerUiState> = _uiState.asStateFlow()

    init {
        if (serverId != -1L) loadServer(serverId)
    }

    private fun loadServer(id: Long) {
        viewModelScope.launch {
            val server = serverRepository.getServerById(id) ?: return@launch
            _uiState.value = AddServerUiState(
                name = server.name,
                host = server.host,
                agentPort = server.agentPort.toString(),
                sshPort = server.sshPort.toString(),
                authType = server.authType,
                agentToken = server.agentToken ?: "",
                sshUsername = server.sshUsername ?: "",
                sshPassword = server.sshPassword ?: "",
                sshPrivateKey = server.sshPrivateKey ?: ""
            )
        }
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onHostChange(value: String) { _uiState.value = _uiState.value.copy(host = value) }
    fun onAgentPortChange(value: String) { _uiState.value = _uiState.value.copy(agentPort = value) }
    fun onSshPortChange(value: String) { _uiState.value = _uiState.value.copy(sshPort = value) }
    fun onAuthTypeChange(value: AuthType) { _uiState.value = _uiState.value.copy(authType = value) }
    fun onAgentTokenChange(value: String) { _uiState.value = _uiState.value.copy(agentToken = value) }
    fun onSshUsernameChange(value: String) { _uiState.value = _uiState.value.copy(sshUsername = value) }
    fun onSshPasswordChange(value: String) { _uiState.value = _uiState.value.copy(sshPassword = value) }
    fun onSshPrivateKeyChange(value: String) { _uiState.value = _uiState.value.copy(sshPrivateKey = value) }

    fun testConnection() {
        val state = _uiState.value
        val server = buildServerProfile(state) ?: return
        _uiState.value = state.copy(isTesting = true, testResult = null)
        viewModelScope.launch {
            val result = serverRepository.testConnection(server)
            _uiState.value = _uiState.value.copy(
                isTesting = false,
                testResult = result.fold(
                    onSuccess = { latency -> "Connected in ${latency}ms" },
                    onFailure = { e -> "Failed: ${e.message}" }
                )
            )
        }
    }

    fun saveServer() {
        val state = _uiState.value
        val server = buildServerProfile(state) ?: run {
            _uiState.value = state.copy(error = "Please fill all required fields")
            return
        }
        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            if (serverId != -1L) serverRepository.updateServer(server.copy(id = serverId))
            else serverRepository.insertServer(server)
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
        }
    }

    private fun buildServerProfile(state: AddServerUiState): ServerProfile? {
        if (state.name.isBlank() || state.host.isBlank()) return null
        return ServerProfile(
            name = state.name,
            host = state.host,
            agentPort = state.agentPort.toIntOrNull() ?: 9876,
            sshPort = state.sshPort.toIntOrNull() ?: 22,
            authType = state.authType,
            agentToken = state.agentToken.takeIf { it.isNotBlank() },
            sshUsername = state.sshUsername.takeIf { it.isNotBlank() },
            sshPassword = state.sshPassword.takeIf { it.isNotBlank() },
            sshPrivateKey = state.sshPrivateKey.takeIf { it.isNotBlank() }
        )
    }
}
