package com.servercontrol.domain.usecase

import com.servercontrol.domain.repository.StatsRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class ToggleFirewallRuleUseCase @Inject constructor(
    private val repository: StatsRepository
) {
    suspend operator fun invoke(serverId: Long, ruleId: String, enable: Boolean): Resource<Unit> =
        repository.toggleFirewallRule(serverId, ruleId, enable)
}
