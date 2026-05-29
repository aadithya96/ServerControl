package com.servercontrol.data.remote.agent

import com.servercontrol.data.remote.dto.*
import com.servercontrol.domain.model.ServiceAction
import com.servercontrol.domain.model.ServerProfile
import com.servercontrol.util.Resource
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class AgentDataSource {

    fun buildApi(serverProfile: ServerProfile): AgentApi {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val okHttpClient = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply {
                        serverProfile.agentToken?.let {
                            addHeader("Authorization", "Bearer $it")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        val scheme = if (serverProfile.agentPort == 443) "https" else "http"
        return Retrofit.Builder()
            .baseUrl("$scheme://${serverProfile.host}:${serverProfile.agentPort}/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgentApi::class.java)
    }

    /**
     * Test connectivity by calling /health and measuring latency.
     * Returns latency in milliseconds on success, or an error.
     */
    suspend fun testConnection(serverProfile: ServerProfile): Resource<Long> {
        return try {
            val start = System.currentTimeMillis()
            buildApi(serverProfile).healthCheck()
            val latency = System.currentTimeMillis() - start
            Resource.Success(latency)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Connection failed", e)
        }
    }

    suspend fun getStats(serverProfile: ServerProfile): Resource<SystemStatsDto> =
        safeCall { buildApi(serverProfile).getStats() }

    suspend fun getProcesses(
        serverProfile: ServerProfile,
        sort: String = "cpu",
        limit: Int = 100
    ): Resource<ProcessResponseDto> =
        safeCall { buildApi(serverProfile).getProcesses(sort, limit) }

    suspend fun getDiskInfo(serverProfile: ServerProfile): Resource<DiskResponseDto> =
        safeCall { buildApi(serverProfile).getDisk() }

    suspend fun getConnections(
        serverProfile: ServerProfile,
        proto: String = "all"
    ): Resource<ConnectionResponseDto> =
        safeCall { buildApi(serverProfile).getConnections(proto) }

    suspend fun getFirewallRules(serverProfile: ServerProfile): Resource<FirewallResponseDto> =
        safeCall { buildApi(serverProfile).getFirewall() }

    suspend fun killProcess(
        serverProfile: ServerProfile,
        pid: Int,
        signal: Int = 9
    ): Resource<KillResponseDto> =
        safeCall { buildApi(serverProfile).killProcess(pid, signal) }

    suspend fun toggleFirewallRule(
        serverProfile: ServerProfile,
        ruleId: String,
        enabled: Boolean
    ): Resource<FirewallToggleResponseDto> =
        safeCall {
            buildApi(serverProfile).toggleFirewallRule(
                FirewallToggleRequest(ruleId, enabled)
            )
        }

    suspend fun getServices(
        serverProfile: ServerProfile,
        type: String? = null,
        state: String? = null
    ): Resource<ServicesResponseDto> =
        safeCall { buildApi(serverProfile).getServices(type, state) }

    suspend fun serviceAction(
        serverProfile: ServerProfile,
        name: String,
        action: ServiceAction
    ): Resource<ServiceActionResponseDto> =
        safeCall {
            buildApi(serverProfile).serviceAction(
                name,
                ServiceActionRequest(action.name.lowercase())
            )
        }

    suspend fun getServiceLogs(
        serverProfile: ServerProfile,
        name: String,
        lines: Int = 100
    ): Resource<ServiceLogsResponseDto> =
        safeCall { buildApi(serverProfile).getServiceLogs(name, lines) }

    suspend fun getLogs(
        serverProfile: ServerProfile,
        source: String = "journal",
        unit: String? = null,
        lines: Int = 200
    ): Resource<LogResponseDto> =
        safeCall { buildApi(serverProfile).getLogs(source, unit, lines) }

    private suspend fun <T> safeCall(call: suspend () -> T): Resource<T> = try {
        Resource.Success(call())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Agent request failed", e)
    }
}
