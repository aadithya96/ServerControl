package com.servercontrol.presentation.firewall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.FirewallRule
import com.servercontrol.domain.usecase.GetFirewallRulesUseCase
import com.servercontrol.domain.usecase.ToggleFirewallRuleUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FirewallUiState(
    val rules: List<FirewallRule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class FirewallViewModel @Inject constructor(
    private val getFirewallRulesUseCase: GetFirewallRulesUseCase,
    private val toggleFirewallRuleUseCase: ToggleFirewallRuleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FirewallUiState())
    val uiState: StateFlow<FirewallUiState> = _uiState.asStateFlow()

    private var serverId: Long = -1L

    fun init(serverId: Long) {
        this.serverId = serverId
        loadRules()
    }

    fun refresh() { loadRules() }

    fun toggleRule(ruleId: String, enable: Boolean) {
        viewModelScope.launch {
            when (val result = toggleFirewallRuleUseCase(serverId, ruleId, enable)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(message = "Rule ${if (enable) "enabled" else "disabled"}")
                    loadRules()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(message = "Error: ${result.message}")
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = getFirewallRulesUseCase(serverId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    rules = result.data, isLoading = false, error = null
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, error = result.message
                )
                is Resource.Loading -> Unit
            }
        }
    }
}
