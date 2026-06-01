package com.servercontrol.util

import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.servercontrol.domain.model.AuthType
import com.servercontrol.domain.model.ServerProfile
import org.json.JSONObject

object QrCodeUtil {

    fun generateQrBitmap(data: String, size: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            // A quiet zone of at least 4 modules is required by the QR spec.
            // A margin of 1 (the previous value) makes the code unscannable on
            // many readers, which is why generated codes "didn't work".
            EncodeHintType.MARGIN to 4
        )
        val matrix = MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
        val width = matrix.width
        val height = matrix.height
        // ARGB_8888 guarantees pure black/white modules. RGB_565 quantizes the
        // colours, which can reduce the contrast scanners rely on.
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] =
                    if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun encodeServerProfile(profile: ServerProfile): String {
        val json = JSONObject().apply {
            put("name", profile.name)
            put("host", profile.host)
            put("agentPort", profile.agentPort)
            put("sshPort", profile.sshPort)
            put("authType", profile.authType.name)
            put("group", profile.group)
            // Include agent token (user should be warned it's sensitive)
            // Do NOT include SSH passwords or private keys
            if (profile.authType == AuthType.AGENT_TOKEN && profile.agentToken != null) {
                put("agentToken", profile.agentToken)
            }
            if (profile.sshUsername != null) {
                put("sshUsername", profile.sshUsername)
            }
        }
        return Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun decodeServerProfile(qrData: String): ServerProfile? {
        return try {
            val json = JSONObject(String(Base64.decode(qrData, Base64.NO_WRAP), Charsets.UTF_8))
            ServerProfile(
                name = json.optString("name", "Imported Server"),
                host = json.optString("host", ""),
                agentPort = json.optInt("agentPort", 9876),
                sshPort = json.optInt("sshPort", 22),
                authType = runCatching { AuthType.valueOf(json.optString("authType", "AGENT_TOKEN")) }
                    .getOrDefault(AuthType.AGENT_TOKEN),
                agentToken = json.optString("agentToken", null),
                sshUsername = json.optString("sshUsername", null),
                group = json.optString("group", "default")
            )
        } catch (e: Exception) {
            null
        }
    }
}
