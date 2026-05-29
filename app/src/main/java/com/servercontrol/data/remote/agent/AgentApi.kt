package com.servercontrol.data.remote.agent

import com.servercontrol.data.remote.dto.ConnectionResponseDto
import com.servercontrol.data.remote.dto.DiskResponseDto
import com.servercontrol.data.remote.dto.FirewallResponseDto
import com.servercontrol.data.remote.dto.FirewallToggleRequest
import com.servercontrol.data.remote.dto.FirewallToggleResponseDto
import com.servercontrol.data.remote.dto.HealthDto
import com.servercontrol.data.remote.dto.KillResponseDto
import com.servercontrol.data.remote.dto.LogResponseDto
import com.servercontrol.data.remote.dto.ProcessResponseDto
import com.servercontrol.data.remote.dto.ServiceActionRequest
import com.servercontrol.data.remote.dto.ServiceActionResponseDto
import com.servercontrol.data.remote.dto.ServiceLogsResponseDto
import com.servercontrol.data.remote.dto.ServicesResponseDto
import com.servercontrol.data.remote.dto.SystemStatsDto
import retrofit2.http.*

interface AgentApi {

    @GET("api/v1/stats")
    suspend fun getStats(): SystemStatsDto

    @GET("api/v1/processes")
    suspend fun getProcesses(
        @Query("sort") sort: String = "cpu",
        @Query("limit") limit: Int = 100
    ): ProcessResponseDto

    @GET("api/v1/disk")
    suspend fun getDisk(): DiskResponseDto

    @GET("api/v1/connections")
    suspend fun getConnections(@Query("proto") proto: String = "all"): ConnectionResponseDto

    @GET("api/v1/firewall")
    suspend fun getFirewall(): FirewallResponseDto

    @DELETE("api/v1/process/{pid}")
    suspend fun killProcess(
        @Path("pid") pid: Int,
        @Query("signal") signal: Int = 9
    ): KillResponseDto

    @POST("api/v1/firewall/toggle")
    suspend fun toggleFirewallRule(@Body request: FirewallToggleRequest): FirewallToggleResponseDto

    @GET("health")
    suspend fun healthCheck(): HealthDto

    @GET("api/v1/services")
    suspend fun getServices(
        @Query("type") type: String? = null,
        @Query("state") state: String? = null
    ): ServicesResponseDto

    @POST("api/v1/services/{name}/action")
    suspend fun serviceAction(
        @Path("name") name: String,
        @Body request: ServiceActionRequest
    ): ServiceActionResponseDto

    @GET("api/v1/services/{name}/logs")
    suspend fun getServiceLogs(
        @Path("name") name: String,
        @Query("lines") lines: Int = 100
    ): ServiceLogsResponseDto

    @GET("api/v1/logs")
    suspend fun getLogs(
        @Query("source") source: String = "journal",
        @Query("unit") unit: String? = null,
        @Query("lines") lines: Int = 200
    ): LogResponseDto
}
