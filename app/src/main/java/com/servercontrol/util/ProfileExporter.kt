package com.servercontrol.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ProfileExporter {

    fun exportToJson(profiles: List<ServerProfile>): String {
        val array = JSONArray()
        for (profile in profiles) {
            val obj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("host", profile.host)
                put("agentPort", profile.agentPort)
                put("sshPort", profile.sshPort)
                put("authType", profile.authType.name)
                put("group", profile.group)
                // Include agent token (sensitive — warn user)
                if (profile.authType == AuthType.AGENT_TOKEN && profile.agentToken != null) {
                    put("agentToken", profile.agentToken)
                }
                // Include SSH username but NOT password or private key
                if (profile.sshUsername != null) {
                    put("sshUsername", profile.sshUsername)
                }
                // sshPassword and sshPrivateKey intentionally excluded for security
            }
            array.put(obj)
        }
        return JSONObject().apply {
            put("version", 1)
            put("app", "ServerControl")
            put("note", "SSH passwords and private keys are not exported for security")
            put("profiles", array)
        }.toString(2)
    }

    fun importFromJson(json: String): List<ServerProfile> {
        return try {
            val root = JSONObject(json)
            val array = root.getJSONArray("profiles")
            (0 until array.length()).mapNotNull { i ->
                try {
                    val obj = array.getJSONObject(i)
                    ServerProfile(
                        name = obj.optString("name", "Imported Server"),
                        host = obj.optString("host", ""),
                        agentPort = obj.optInt("agentPort", 9876),
                        sshPort = obj.optInt("sshPort", 22),
                        authType = runCatching { AuthType.valueOf(obj.optString("authType", "AGENT_TOKEN")) }
                            .getOrDefault(AuthType.AGENT_TOKEN),
                        agentToken = obj.optString("agentToken").takeIf { it.isNotBlank() },
                        sshUsername = obj.optString("sshUsername").takeIf { it.isNotBlank() },
                        group = obj.optString("group", "default")
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun shareProfiles(context: Context, profiles: List<ServerProfile>) {
        val json = exportToJson(profiles)
        val file = File(context.cacheDir, "servercontrol_profiles.json").also { it.writeText(json) }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, "ServerControl Profiles Export")
        }
        context.startActivity(Intent.createChooser(intent, "Export Server Profiles"))
    }
}
