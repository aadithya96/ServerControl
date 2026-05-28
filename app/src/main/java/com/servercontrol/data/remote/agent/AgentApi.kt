package com.servercontrol.data.remote.agent

import com.servercontrol.data.remote.dto.ConnectionDto
import com.servercontrol.data.remote.dto.DiskDto
import com.servercontrol.data.remote.dto.FirewallRuleDto
import com.servercontrol.data.remote.dto.ProcessDto
import com.servercontrol.data.remote.dto.SystemStatsDto
import retrofit2.http.*

interface AgentApi {

    @GET("stats")
    suspend fun getSystemStats(): SystemStatsDto

    @GET("processes")
    suspend fun getProcesses(): List<ProcessDto>

    @GET("disk")
    suspend fun getDiskInfo(): List<DiskDto>

    @GET("connections")
    suspend fun getConnections(): List<ConnectionDto>

    @GET("firewall")
    suspend fun getFirewallRules(): List<FirewallRuleDto>

    @DELETE("process/{pid}")
    suspend fun killProcess(@Path("pid") pid: Int)

    @POST("firewall/toggle")
    suspend fun toggleFirewallRule(@Body request: FirewallToggleRequest)
}

data class FirewallToggleRequest(
    val id: String,
    val enable: Boolean
)
