package com.servercontrol.presentation.logs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.LogEntry
import com.servercontrol.domain.model.LogSource
import com.servercontrol.domain.usecase.GetLogsUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewerViewModel @Inject constructor(
    private val getLogsUseCase: GetLogsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: -1L

    val selectedSource = MutableStateFlow("journal")
    val selectedUnit = MutableStateFlow<String?>(null)
    val lineCount = MutableStateFlow(200)
    val searchQuery = MutableStateFlow("")
    val autoRefresh = MutableStateFlow(false)
    val autoScroll = MutableStateFlow(true)

    private val _logs = MutableStateFlow<Resource<List<LogEntry>>>(Resource.Loading)

    val filteredLogs: StateFlow<Resource<List<LogEntry>>> = combine(
        _logs, searchQuery
    ) { resource, query ->
        when (resource) {
            is Resource.Success -> {
                if (query.isBlank()) resource
                else {
                    val q = query.lowercase()
                    Resource.Success(resource.data.filter {
                        it.message.lowercase().contains(q) ||
                            it.source.lowercase().contains(q) ||
                            it.rawLine.lowercase().contains(q)
                    })
                }
            }
            else -> resource
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading)

    val availableSources = listOf(
        LogSource("journal", "System Journal", "journal"),
        LogSource("syslog", "/var/log/syslog", "syslog"),
        LogSource("auth", "Auth Log", "auth"),
        LogSource("nginx", "Nginx Error", "nginx"),
        LogSource("apache", "Apache Error", "apache"),
        LogSource("custom", "Custom Path…", "custom")
    )

    private var refreshJob: Job? = null

    init {
        loadLogs()
    }

    fun setSource(source: String, unit: String? = null) {
        selectedSource.value = source
        selectedUnit.value = unit
        loadLogs()
    }

    fun setLineCount(n: Int) {
        lineCount.value = n
        loadLogs()
    }

    fun setSearchQuery(q: String) {
        searchQuery.value = q
    }

    fun toggleAutoRefresh() {
        autoRefresh.value = !autoRefresh.value
        if (autoRefresh.value) startAutoRefresh() else stopAutoRefresh()
    }

    fun toggleAutoScroll() {
        autoScroll.value = !autoScroll.value
    }

    fun refresh() {
        loadLogs()
    }

    fun exportLogs(): String {
        return (_logs.value as? Resource.Success)?.data?.joinToString("\n") { it.rawLine } ?: ""
    }

    private fun loadLogs() {
        viewModelScope.launch {
            getLogsUseCase(serverId, selectedSource.value, selectedUnit.value, lineCount.value)
                .collect { result ->
                    _logs.value = result
                }
        }
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (autoRefresh.value) {
                delay(2000L)
                loadLogs()
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
