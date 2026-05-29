package com.servercontrol.presentation.docker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.DockerAction
import com.servercontrol.domain.model.DockerContainer
import com.servercontrol.domain.model.DockerImage
import com.servercontrol.domain.usecase.DockerContainerActionUseCase
import com.servercontrol.domain.usecase.GetDockerContainerLogsUseCase
import com.servercontrol.domain.usecase.GetDockerContainersUseCase
import com.servercontrol.domain.usecase.GetDockerImagesUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DockerViewModel @Inject constructor(
    private val getDockerContainersUseCase: GetDockerContainersUseCase,
    private val getDockerImagesUseCase: GetDockerImagesUseCase,
    private val dockerContainerActionUseCase: DockerContainerActionUseCase,
    private val getDockerContainerLogsUseCase: GetDockerContainerLogsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: 0L

    val selectedTab = MutableStateFlow(0)

    private val _containers = MutableStateFlow<Resource<List<DockerContainer>>>(Resource.Loading)
    val containers: StateFlow<Resource<List<DockerContainer>>> = _containers.asStateFlow()

    private val _images = MutableStateFlow<Resource<List<DockerImage>>>(Resource.Loading)
    val images: StateFlow<Resource<List<DockerImage>>> = _images.asStateFlow()

    val actionResult: MutableStateFlow<Resource<String>?> = MutableStateFlow(null)
    val containerLogs: MutableStateFlow<Resource<List<String>>?> = MutableStateFlow(null)
    val selectedContainer: MutableStateFlow<DockerContainer?> = MutableStateFlow(null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _containers.value = Resource.Loading
            _containers.value = getDockerContainersUseCase(serverId)
        }
        viewModelScope.launch {
            _images.value = Resource.Loading
            _images.value = getDockerImagesUseCase(serverId)
        }
    }

    fun performAction(containerId: String, action: DockerAction) {
        viewModelScope.launch {
            actionResult.value = Resource.Loading
            actionResult.value = dockerContainerActionUseCase(serverId, containerId, action)
            // Refresh containers after action
            _containers.value = getDockerContainersUseCase(serverId)
        }
    }

    fun loadLogs(containerId: String) {
        viewModelScope.launch {
            containerLogs.value = Resource.Loading
            containerLogs.value = getDockerContainerLogsUseCase(serverId, containerId, 100)
        }
    }

    fun selectContainer(c: DockerContainer?) {
        selectedContainer.value = c
        if (c == null) containerLogs.value = null
    }

    fun clearActionResult() {
        actionResult.value = null
    }
}
