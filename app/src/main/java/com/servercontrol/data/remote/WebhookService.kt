package com.servercontrol.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    suspend fun sendSlackAlert(webhookUrl: String, message: String, serverName: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("text", "⚠️ *$serverName*: $message").toString()
        post(webhookUrl, payload)
    }

    suspend fun sendDiscordAlert(webhookUrl: String, message: String, serverName: String) = withContext(Dispatchers.IO) {
        val embed = JSONObject()
            .put("title", serverName)
            .put("description", message)
            .put("color", 16744272) // orange-red
        val payload = JSONObject()
            .put("content", "⚠️ **$serverName**: $message")
            .put("embeds", JSONArray().put(embed))
            .toString()
        post(webhookUrl, payload)
    }

    suspend fun sendTelegramAlert(botToken: String, chatId: String, message: String, serverName: String) = withContext(Dispatchers.IO) {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val payload = JSONObject()
            .put("chat_id", chatId)
            .put("text", "⚠️ *$serverName*: $message")
            .put("parse_mode", "Markdown")
            .toString()
        post(url, payload)
    }

    private fun post(url: String, body: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody(JSON_MEDIA))
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(response.body?.string() ?: "")
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
