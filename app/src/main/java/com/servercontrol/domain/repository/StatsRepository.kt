package com.servercontrol.domain.repository

import com.servercontrol.domain.model.BandwidthInfo
import com.servercontrol.domain.model.DiskInfo
import com.servercontrol.domain.model.FirewallData
import com.servercontrol.domain.model.LogEntry
import com.servercontrol.domain.model.NetworkConnection
import com.servercontrol.domain.model.Process
import com.servercontrol.domain.model.ServiceAction
import com.servercontrol.domain.model.SystemService
import com.servercontrol.domain.model.SystemStats
import com.servercontrol.util.Resource

interface StatsRepository {
    suspend fun getSystemStats(serverId: Long): Resource<SystemStats>
    suspend fun getBandwidth(serverId: Long): Resource<List<BandwidthInfo>>
    suspend fun getProcesses(serverId: Long): Resource<List<Process>>
    suspend fun getDiskInfo(serverId: Long): Resource<List<DiskInfo>>
    suspend fun getConnections(serverId: Long): Resource<List<NetworkConnection>>
    suspend fun getFirewallRules(serverId: Long): Resource<FirewallData>
    suspend fun killProcess(serverId: Long, pid: Int): Resource<Unit>
    suspend fun toggleFirewallRule(serverId: Long, ruleId: String, enable: Boolean): Resource<Unit>
    suspend fun getServices(serverId: Long, type: String? = null, state: String? = null): Resource<List<SystemService>>
    suspend fun serviceAction(serverId: Long, name: String, action: ServiceAction): Resource<String>
    suspend fun getServiceLogs(serverId: Long, name: String, lines: Int): Resource<List<String>>
    suspend fun getLogs(serverId: Long, source: String, unit: String?, lines: Int): Resource<List<LogEntry>>
}
