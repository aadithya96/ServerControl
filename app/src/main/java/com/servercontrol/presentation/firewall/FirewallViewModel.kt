package com.servercontrol.presentation.firewall

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.domain.model.FirewallData
import com.servercontrol.domain.usecase.GetFirewallRulesUseCase
import com.servercontrol.domain.usecase.ToggleFirewallRuleUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirewallViewModel @Inject constructor(
    private val getFirewallRulesUseCase: GetFirewallRulesUseCase,
    private val toggleFirewallRuleUseCase: ToggleFirewallRuleUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle.get<Long>("serverId") ?: -1L

    private val _firewallData = MutableStateFlow<Resource<FirewallData>>(Resource.Loading)
    val firewallData: StateFlow<Resource<FirewallData>> = _firewallData.asStateFlow()

    val toggleResult: MutableStateFlow<Resource<String>?> = MutableStateFlow(null)

    val expandedChains: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    init {
        refresh()
    }

    fun toggleChainExpanded(chainName: String) {
        val current = expandedChains.value
        expandedChains.value = if (chainName in current) {
            current - chainName
        } else {
            current + chainName
        }
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            val result = toggleFirewallRuleUseCase(serverId, ruleId, enabled)
            toggleResult.value = when (result) {
                is Resource.Success -> Resource.Success("Rule ${if (enabled) "enabled" else "disabled"} successfully")
                is Resource.Error -> Resource.Error(result.message)
                is Resource.Loading -> null
            }
            if (result is Resource.Success) {
                refresh()
            }
        }
    }

    fun clearToggleResult() {
        toggleResult.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            _firewallData.value = Resource.Loading
            _firewallData.value = getFirewallRulesUseCase(serverId)
            // Expand all chains by default on first load
            val data = _firewallData.value
            if (data is Resource.Success && expandedChains.value.isEmpty()) {
                expandedChains.value = data.data.chains.map { it.name }.toSet()
            }
        }
    }
}
