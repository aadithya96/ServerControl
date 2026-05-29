package com.servercontrol.presentation.services

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.ServiceAction
import com.servercontrol.domain.model.SystemService
import com.servercontrol.domain.usecase.GetServiceLogsUseCase
import com.servercontrol.domain.usecase.GetServicesUseCase
import com.servercontrol.domain.usecase.ServiceActionUseCase
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
class ServiceManagerViewModel @Inject constructor(
    private val getServicesUseCase: GetServicesUseCase,
    private val serviceActionUseCase: ServiceActionUseCase,
    private val getServiceLogsUseCase: GetServiceLogsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: -1L

    private val _allServices = MutableStateFlow<Resource<List<SystemService>>>(Resource.Loading)
    val stateFilter = MutableStateFlow("all")
    val typeFilter = MutableStateFlow("all")
    val searchQuery = MutableStateFlow("")

    val services: StateFlow<Resource<List<SystemService>>> = combine(
        _allServices, stateFilter, typeFilter, searchQuery
    ) { resource, state, type, query ->
        when (resource) {
            is Resource.Success -> {
                var filtered = resource.data
                if (state != "all") filtered = filtered.filter { it.activeState == state }
                if (type != "all") filtered = filtered.filter { it.type == type }
                if (query.isNotBlank()) {
                    val q = query.lowercase()
                    filtered = filtered.filter {
                        it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
                    }
                }
                Resource.Success(filtered)
            }
            else -> resource
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading)

    val actionResult: MutableStateFlow<Resource<String>?> = MutableStateFlow(null)
    val selectedService: MutableStateFlow<SystemService?> = MutableStateFlow(null)
    val serviceLogs: MutableStateFlow<Resource<List<String>>?> = MutableStateFlow(null)

    init {
        loadServices()
    }

    fun setStateFilter(f: String) { stateFilter.value = f }
    fun setTypeFilter(f: String) { typeFilter.value = f }
    fun setSearchQuery(q: String) { searchQuery.value = q }
    fun selectService(s: SystemService?) {
        selectedService.value = s
        serviceLogs.value = null
    }

    fun performAction(name: String, action: ServiceAction) {
        viewModelScope.launch {
            serviceActionUseCase(serverId, name, action).collect { result ->
                actionResult.value = result
            }
            // Reload after action
            if (actionResult.value is Resource.Success) {
                loadServices()
            }
        }
    }

    fun loadLogs(name: String) {
        viewModelScope.launch {
            getServiceLogsUseCase(serverId, name, 200).collect { result ->
                serviceLogs.value = result
            }
        }
    }

    fun refresh() {
        loadServices()
    }

    private fun loadServices() {
        viewModelScope.launch {
            getServicesUseCase(serverId).collect { result ->
                _allServices.value = result
            }
        }
    }
}
