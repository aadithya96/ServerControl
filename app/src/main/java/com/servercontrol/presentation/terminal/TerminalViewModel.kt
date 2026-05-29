package com.servercontrol.presentation.terminal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.servercontrol.domain.repository.ServerRepository
import com.servercontrol.terminal.AnsiParser
import com.servercontrol.terminal.SessionState
import com.servercontrol.terminal.TerminalColorTheme
import com.servercontrol.terminal.TerminalManager
import com.servercontrol.terminal.TerminalSession
import com.servercontrol.terminal.TerminalThemes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private val KEY_FONT_SIZE = intPreferencesKey("terminal_font_size")
private val KEY_COLOR_THEME = stringPreferencesKey("terminal_theme")

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val terminalManager: TerminalManager,
    private val serverRepository: ServerRepository,
    private val dataStore: DataStore<Preferences>,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle.get<Long>("serverId") ?: 0L

    private val _tabs = MutableStateFlow<List<TerminalSession>>(emptyList())
    val tabs: StateFlow<List<TerminalSession>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _outputs = MutableStateFlow<Map<String, AnnotatedString>>(emptyMap())
    val outputs: StateFlow<Map<String, AnnotatedString>> = _outputs.asStateFlow()

    private val _sessionStates = MutableStateFlow<Map<String, SessionState>>(emptyMap())
    val sessionStates: StateFlow<Map<String, SessionState>> = _sessionStates.asStateFlow()

    private val _fontSize = MutableStateFlow(13)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _colorTheme = MutableStateFlow(TerminalColorTheme.DARK)
    val colorTheme: StateFlow<TerminalColorTheme> = _colorTheme.asStateFlow()

    private val collectionJobs = mutableMapOf<String, Job>()
    private var tabCounter = 0

    // Buffer to accumulate partial ANSI sequences between chunks
    private val rawBuffers = mutableMapOf<String, StringBuilder>()

    init {
        // Load persisted preferences, then open first tab
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            _fontSize.value = (prefs[KEY_FONT_SIZE] ?: 13).coerceIn(8, 32)
            _colorTheme.value = try {
                TerminalColorTheme.valueOf(prefs[KEY_COLOR_THEME] ?: "DARK")
            } catch (_: IllegalArgumentException) {
                TerminalColorTheme.DARK
            }
            addTab()
        }
    }

    fun addTab() {
        tabCounter++
        val session = TerminalSession(
            title = "Terminal $tabCounter",
            serverId = serverId
        )
        _tabs.value = _tabs.value + session
        _activeTabId.value = session.id
        setSessionState(session.id, SessionState.CONNECTING)
        connectSession(session)
    }

    private fun connectSession(session: TerminalSession) {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = serverRepository.getServerById(serverId) ?: run {
                setSessionState(session.id, SessionState.ERROR)
                appendOutput(session.id, "\r\n[Error: Server profile not found]\r\n")
                return@launch
            }

            setSessionState(session.id, SessionState.CONNECTING)
            val result = terminalManager.openSession(profile, session.id)

            if (result.isFailure) {
                setSessionState(session.id, SessionState.ERROR)
                appendOutput(
                    session.id,
                    "\r\n[Connection failed: ${result.exceptionOrNull()?.message}]\r\n"
                )
                return@launch
            }

            setSessionState(session.id, SessionState.CONNECTED)
            rawBuffers[session.id] = StringBuilder()

            val job = viewModelScope.launch {
                terminalManager.outputFlow(session.id).collect { chunk ->
                    processChunk(session.id, chunk)
                    if (!terminalManager.isConnected(session.id)) {
                        setSessionState(session.id, SessionState.DISCONNECTED)
                    }
                }
                // Flow ended means session disconnected
                setSessionState(session.id, SessionState.DISCONNECTED)
            }
            collectionJobs[session.id] = job
        }
    }

    private fun processChunk(sessionId: String, chunk: String) {
        val buffer = rawBuffers.getOrPut(sessionId) { StringBuilder() }
        buffer.append(chunk)

        // Process complete lines plus any partial data (we parse the whole buffer each time)
        // but we keep a cap to avoid unbounded growth
        val text = buffer.toString()

        // Parse with current theme's color scheme
        val parsed = AnsiParser.parse(text, TerminalThemes.forTheme(_colorTheme.value))
        buffer.clear()

        // Append to session output, keeping max ~50000 chars of annotated string
        val current = _outputs.value[sessionId] ?: AnnotatedString("")
        val combined = buildAnnotatedString {
            // Trim from start if too long (keep last 50000 chars)
            val startOffset = if (current.length + parsed.length > 50000) {
                (current.length + parsed.length - 50000).coerceAtLeast(0)
            } else 0
            append(current.subSequence(startOffset, current.length))
            append(parsed)
        }

        _outputs.value = _outputs.value.toMutableMap().also { it[sessionId] = combined }
    }

    private fun appendOutput(sessionId: String, text: String) {
        val current = _outputs.value[sessionId] ?: AnnotatedString("")
        val combined = buildAnnotatedString {
            append(current)
            append(text)
        }
        _outputs.value = _outputs.value.toMutableMap().also { it[sessionId] = combined }
    }

    private fun setSessionState(sessionId: String, state: SessionState) {
        _sessionStates.value = _sessionStates.value.toMutableMap().also { it[sessionId] = state }
    }

    fun closeTab(sessionId: String) {
        collectionJobs.remove(sessionId)?.cancel()
        rawBuffers.remove(sessionId)
        terminalManager.closeSession(sessionId)

        val newTabs = _tabs.value.filter { it.id != sessionId }
        _tabs.value = newTabs
        val newOutputs = _outputs.value.toMutableMap().also { it.remove(sessionId) }
        _outputs.value = newOutputs
        val newStates = _sessionStates.value.toMutableMap().also { it.remove(sessionId) }
        _sessionStates.value = newStates

        if (_activeTabId.value == sessionId) {
            _activeTabId.value = newTabs.lastOrNull()?.id
        }
    }

    fun selectTab(sessionId: String) {
        _activeTabId.value = sessionId
    }

    fun sendInput(text: String) {
        val id = _activeTabId.value ?: return
        terminalManager.sendInput(id, text)
    }

    fun sendControlChar(char: Char) {
        val id = _activeTabId.value ?: return
        terminalManager.sendInput(id, char.toString())
    }

    fun setFontSize(size: Int) {
        _fontSize.value = size.coerceIn(8, 32)
        viewModelScope.launch {
            dataStore.edit { it[KEY_FONT_SIZE] = _fontSize.value }
        }
    }

    fun increaseFontSize() {
        setFontSize(_fontSize.value + 1)
    }

    fun decreaseFontSize() {
        setFontSize(_fontSize.value - 1)
    }

    fun setColorTheme(theme: TerminalColorTheme) {
        _colorTheme.value = theme
        viewModelScope.launch {
            dataStore.edit { it[KEY_COLOR_THEME] = theme.name }
        }
    }

    fun reconnect(sessionId: String) {
        val session = _tabs.value.find { it.id == sessionId } ?: return
        collectionJobs.remove(sessionId)?.cancel()
        rawBuffers.remove(sessionId)
        terminalManager.closeSession(sessionId)
        setSessionState(sessionId, SessionState.CONNECTING)
        // Clear output for this tab
        _outputs.value = _outputs.value.toMutableMap().also { it[sessionId] = AnnotatedString("") }
        connectSession(session)
    }

    override fun onCleared() {
        collectionJobs.values.forEach { it.cancel() }
        terminalManager.closeAllSessions()
        super.onCleared()
    }
}
