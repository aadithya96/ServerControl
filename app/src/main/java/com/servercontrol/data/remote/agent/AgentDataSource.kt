package com.servercontrol.data.remote.agent

import com.servercontrol.data.remote.dto.*
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

        return Retrofit.Builder()
            .baseUrl("http://${serverProfile.host}:${serverProfile.agentPort}/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgentApi::class.java)
    }

    suspend fun getSystemStats(serverProfile: ServerProfile): Resource<SystemStatsDto> =
        safeCall { buildApi(serverProfile).getSystemStats() }

    suspend fun getProcesses(serverProfile: ServerProfile): Resource<List<ProcessDto>> =
        safeCall { buildApi(serverProfile).getProcesses() }

    suspend fun getDiskInfo(serverProfile: ServerProfile): Resource<List<DiskDto>> =
        safeCall { buildApi(serverProfile).getDiskInfo() }

    suspend fun getConnections(serverProfile: ServerProfile): Resource<List<ConnectionDto>> =
        safeCall { buildApi(serverProfile).getConnections() }

    suspend fun getFirewallRules(serverProfile: ServerProfile): Resource<List<FirewallRuleDto>> =
        safeCall { buildApi(serverProfile).getFirewallRules() }

    suspend fun killProcess(serverProfile: ServerProfile, pid: Int): Resource<Unit> =
        safeCall { buildApi(serverProfile).killProcess(pid) }

    suspend fun toggleFirewallRule(
        serverProfile: ServerProfile,
        ruleId: String,
        enable: Boolean
    ): Resource<Unit> =
        safeCall { buildApi(serverProfile).toggleFirewallRule(FirewallToggleRequest(ruleId, enable)) }

    private suspend fun <T> safeCall(call: suspend () -> T): Resource<T> = try {
        Resource.Success(call())
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Agent request failed", e)
    }
}
