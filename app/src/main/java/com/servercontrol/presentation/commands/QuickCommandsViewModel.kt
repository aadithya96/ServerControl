package com.servercontrol.presentation.commands

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.SavedCommand
import com.servercontrol.domain.repository.CommandRepository
import com.servercontrol.domain.usecase.GetCommandsUseCase
import com.servercontrol.domain.usecase.RunCommandUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuickCommandsViewModel @Inject constructor(
    private val getCommandsUseCase: GetCommandsUseCase,
    private val runCommandUseCase: RunCommandUseCase,
    private val commandRepository: CommandRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: 0L

    val searchQuery = MutableStateFlow("")
    val showAddDialog = MutableStateFlow(false)
    val runResult: MutableStateFlow<Pair<String, Resource<String>>?> = MutableStateFlow(null)

    var newName by mutableStateOf("")
    var newCommand by mutableStateOf("")
    var newDescription by mutableStateOf("")

    private val allCommands: StateFlow<List<SavedCommand>> = getCommandsUseCase(serverId.toString())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commands: StateFlow<List<SavedCommand>> = combine(allCommands, searchQuery) { cmds, query ->
        if (query.isBlank()) cmds
        else cmds.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.command.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { commandRepository.seedBuiltIns() }
    }

    fun runCommand(command: SavedCommand) {
        viewModelScope.launch {
            runCommandUseCase(serverId, command.command).collect { result ->
                runResult.value = command.name to result
            }
        }
    }

    fun saveCommand() {
        if (newName.isBlank() || newCommand.isBlank()) return
        viewModelScope.launch {
            commandRepository.saveCommand(
                SavedCommand(
                    id = UUID.randomUUID().toString(),
                    serverId = null,
                    name = newName.trim(),
                    command = newCommand.trim(),
                    description = newDescription.trim(),
                    isBuiltIn = false
                )
            )
            newName = ""
            newCommand = ""
            newDescription = ""
            showAddDialog.value = false
        }
    }

    fun deleteCommand(id: String) {
        viewModelScope.launch { commandRepository.deleteCommand(id) }
    }

    fun clearResult() {
        runResult.value = null
    }

    fun exportCommands() {
        viewModelScope.launch { commandRepository.exportJson() }
    }
}
