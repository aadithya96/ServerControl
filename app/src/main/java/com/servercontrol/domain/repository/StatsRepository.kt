package com.servercontrol.domain.repository

import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.domain.model.FirewallData
import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.util.Resource

interface StatsRepository {
    suspend fun getSystemStats(serverId: Long): Resource<SystemStats>
    suspend fun getProcesses(serverId: Long): Resource<List<Process>>
    suspend fun getDiskInfo(serverId: Long): Resource<List<DiskInfo>>
    suspend fun getConnections(serverId: Long): Resource<List<NetworkConnection>>
    suspend fun getFirewallRules(serverId: Long): Resource<FirewallData>
    suspend fun killProcess(serverId: Long, pid: Int): Resource<Unit>
    suspend fun toggleFirewallRule(serverId: Long, ruleId: String, enable: Boolean): Resource<Unit>
}
