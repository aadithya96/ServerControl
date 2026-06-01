package com.servercontrol.presentation.servers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.domain.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class AgentInstallerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val serverId: Long = savedStateHandle.get<Long>("serverId") ?: 0L

    sealed class InstallerState {
        data object Idle : InstallerState()
        data object CheckingPrereqs : InstallerState()
        data class Ready(
            val hasAgent: Boolean,
            val agentVersion: String?,
            val hasCurl: Boolean,
            val hasSystemd: Boolean
        ) : InstallerState()
        data object Installing : InstallerState()
        data class StreamingOutput(val lines: List<String>, val isComplete: Boolean) : InstallerState()
        data class Success(val token: String, val port: Int) : InstallerState()
        data class Error(val message: String, val logs: List<String> = emptyList()) : InstallerState()
    }

    private val _state = MutableStateFlow<InstallerState>(InstallerState.Idle)
    val state: StateFlow<InstallerState> = _state.asStateFlow()

    val outputLines = MutableStateFlow<List<String>>(emptyList())

    private val _serverName = MutableStateFlow("")
    val serverName: StateFlow<String> = _serverName.asStateFlow()

    private val _serverHost = MutableStateFlow("")
    val serverHost: StateFlow<String> = _serverHost.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = serverRepository.getServerById(serverId)
            _serverName.value = profile?.name ?: ""
            _serverHost.value = profile?.host ?: ""
        }
        checkPrereqs()
    }

    fun checkPrereqs() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = InstallerState.CheckingPrereqs
            val profile = serverRepository.getServerById(serverId) ?: run {
                _state.value = InstallerState.Error("Server not found")
                return@launch
            }

            if (profile.authType == AuthType.AGENT_TOKEN) {
                _state.value = InstallerState.Error("Agent installer requires SSH credentials (SSH Password or SSH Key auth type)")
                return@launch
            }

            runCatching {
                val session = openJschSession(profile)
                try {
                    val curlCheck = execCommand(session, "which curl 2>/dev/null && echo OK || echo MISSING")
                    val hasCurl = curlCheck.contains("OK")

                    val systemdCheck = execCommand(session, "which systemctl 2>/dev/null && echo OK || echo MISSING")
                    val hasSystemd = systemdCheck.contains("OK")

                    val agentCheck = execCommand(session,
                        "systemctl is-active servercontrol-agent 2>/dev/null; " +
                        "/usr/local/bin/servercontrol-agent --version 2>/dev/null || echo NOT_INSTALLED"
                    )
                    val hasAgent = !agentCheck.contains("NOT_INSTALLED") && !agentCheck.contains("No such file")
                    val agentVersion = if (hasAgent) {
                        Regex("v?\\d+\\.\\d+\\.\\d+").find(agentCheck)?.value
                    } else null

                    _state.value = InstallerState.Ready(
                        hasAgent = hasAgent,
                        agentVersion = agentVersion,
                        hasCurl = hasCurl,
                        hasSystemd = hasSystemd
                    )
                } finally {
                    session.disconnect()
                }
            }.onFailure { e ->
                _state.value = InstallerState.Error("Prereq check failed: ${e.message}")
            }
        }
    }

    fun install() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = InstallerState.Installing
            outputLines.value = emptyList()

            val profile = serverRepository.getServerById(serverId) ?: run {
                _state.value = InstallerState.Error("Server not found")
                return@launch
            }

            runCatching {
                val session = openJschSession(profile)
                try {
                    val channel = session.openChannel("exec") as ChannelExec
                    channel.setCommand(
                        "curl -sSL https://raw.githubusercontent.com/aadithya96/ServerControl/main/agent/install.sh | sudo bash 2>&1"
                    )
                    channel.setPty(true)
                    val inputStream = channel.inputStream
                    channel.connect()

                    val buffer = ByteArray(1024)
                    val lineBuffer = StringBuilder()
                    var token: String? = null
                    var port = 9876

                    while (true) {
                        val available = inputStream.available()
                        if (available > 0) {
                            val n = inputStream.read(buffer, 0, min(available, buffer.size))
                            if (n > 0) {
                                val chunk = String(buffer, 0, n, Charsets.UTF_8)
                                lineBuffer.append(chunk)

                                // Extract complete lines
                                while (lineBuffer.contains('\n')) {
                                    val newlineIdx = lineBuffer.indexOf('\n')
                                    val line = lineBuffer.substring(0, newlineIdx).trimEnd('\r')
                                    lineBuffer.delete(0, newlineIdx + 1)

                                    // Check for token
                                    if (line.contains("Token:", ignoreCase = true)) {
                                        val tokenMatch = Regex("Token:\\s*(\\S+)").find(line)
                                        if (tokenMatch != null) {
                                            token = tokenMatch.groupValues[1]
                                        }
                                    }
                                    // Check for port
                                    if (line.contains("Port:", ignoreCase = true) ||
                                        line.contains("port", ignoreCase = true) && line.contains(Regex("\\d{4,5}"))
                                    ) {
                                        val portMatch = Regex("(\\d{4,5})").find(line)
                                        portMatch?.groupValues?.get(1)?.toIntOrNull()?.let {
                                            if (it in 1024..65535) port = it
                                        }
                                    }

                                    val currentLines = outputLines.value + line
                                    outputLines.value = currentLines
                                    _state.value = InstallerState.StreamingOutput(currentLines, false)
                                }
                            }
                        } else if (channel.isClosed) {
                            // Flush remaining
                            if (lineBuffer.isNotEmpty()) {
                                val line = lineBuffer.toString().trimEnd('\r', '\n')
                                if (line.isNotEmpty()) {
                                    val currentLines = outputLines.value + line
                                    outputLines.value = currentLines
                                    _state.value = InstallerState.StreamingOutput(currentLines, false)
                                }
                            }
                            break
                        } else {
                            delay(50)
                        }
                    }

                    channel.disconnect()

                    val exitCode = channel.exitStatus
                    if (exitCode == 0 || token != null) {
                        val finalToken = token ?: "UNKNOWN"
                        _state.value = InstallerState.Success(finalToken, port)
                    } else {
                        _state.value = InstallerState.Error(
                            "Installation failed with exit code $exitCode",
                            outputLines.value
                        )
                    }
                } finally {
                    session.disconnect()
                }
            }.onFailure { e ->
                _state.value = InstallerState.Error("Installation error: ${e.message}")
            }
        }
    }

    fun uninstall() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = InstallerState.Installing
            outputLines.value = listOf("Uninstalling agent...")

            val profile = serverRepository.getServerById(serverId) ?: run {
                _state.value = InstallerState.Error("Server not found")
                return@launch
            }

            runCatching {
                val session = openJschSession(profile)
                try {
                    val result = execCommand(session,
                        "sudo systemctl stop servercontrol-agent 2>&1; " +
                        "sudo systemctl disable servercontrol-agent 2>&1; " +
                        "sudo rm -f /usr/local/bin/servercontrol-agent 2>&1; " +
                        "sudo rm -f /etc/systemd/system/servercontrol-agent.service 2>&1; " +
                        "sudo systemctl daemon-reload 2>&1; echo 'Uninstall complete'"
                    )
                    val lines = result.lines().filter { it.isNotBlank() }
                    outputLines.value = lines
                    _state.value = InstallerState.StreamingOutput(lines, true)
                } finally {
                    session.disconnect()
                }
            }.onFailure { e ->
                _state.value = InstallerState.Error("Uninstall error: ${e.message}")
            }
        }
    }

    fun copyToken(token: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Agent Token", token))
    }

    fun applyToken(token: String, port: Int) {
        viewModelScope.launch {
            val profile = serverRepository.getServerById(serverId) ?: return@launch
            serverRepository.updateServer(
                profile.copy(
                    authType = AuthType.AGENT_TOKEN,
                    agentToken = token,
                    agentPort = port
                )
            )
        }
    }

    private fun openJschSession(profile: ServerProfile): com.jcraft.jsch.Session {
        val jsch = JSch()
        val session = when (profile.authType) {
            AuthType.SSH_KEY -> {
                profile.sshPrivateKey?.let { key ->
                    jsch.addIdentity("key_installer", key.toByteArray(), null, null)
                }
                jsch.getSession(profile.sshUsername ?: "root", profile.host, profile.sshPort)
            }
            AuthType.SSH_PASSWORD -> {
                jsch.getSession(profile.sshUsername ?: "root", profile.host, profile.sshPort).also {
                    it.setPassword(profile.sshPassword ?: "")
                }
            }
            else -> throw IllegalArgumentException("SSH credentials required for agent installer")
        }
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect(15_000)
        return session
    }

    private fun execCommand(session: com.jcraft.jsch.Session, cmd: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(cmd)
        val output = java.io.ByteArrayOutputStream()
        val err = java.io.ByteArrayOutputStream()
        channel.outputStream = output
        channel.setErrStream(err)
        channel.connect(5_000)
        while (!channel.isClosed) Thread.sleep(50)
        channel.disconnect()
        return output.toString(Charsets.UTF_8.name()) + err.toString(Charsets.UTF_8.name())
    }
}
