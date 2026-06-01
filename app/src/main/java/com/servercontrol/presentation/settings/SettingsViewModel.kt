package com.servercontrol.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.data.remote.WebhookService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

object SettingsKeys {
    val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val CPU_ALERT = intPreferencesKey("cpu_alert_threshold")
    val DISK_ALERT = intPreferencesKey("disk_alert_threshold")
    val BG_MONITORING = booleanPreferencesKey("bg_monitoring")
    val BG_INTERVAL = intPreferencesKey("bg_interval_minutes")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val WEBHOOK_TYPE = stringPreferencesKey("webhook_type")
    val WEBHOOK_URL = stringPreferencesKey("webhook_url")
    val TELEGRAM_BOT_TOKEN = stringPreferencesKey("telegram_bot_token")
    val TELEGRAM_CHAT_ID = stringPreferencesKey("telegram_chat_id")
    val ALERT_NOTIFICATIONS = booleanPreferencesKey("alert_notifications")
    val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    val VPN_DETECTION = booleanPreferencesKey("vpn_detection")
    val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
}

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val refreshInterval: Int = 5,
    val cpuAlertThreshold: Int = 80,
    val diskAlertThreshold: Int = 90,
    val backgroundMonitoringEnabled: Boolean = false,
    val backgroundMonitoringInterval: Int = 15,
    val onboardingDone: Boolean = false,
    val webhookType: String = "none",
    val webhookUrl: String = "",
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val alertNotificationsEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val vpnDetectionEnabled: Boolean = false,
    val profileDisplayName: String = "Admin"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val webhookService: WebhookService
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = dataStore.data.map { prefs ->
        SettingsUiState(
            isDarkTheme = prefs[SettingsKeys.DARK_THEME] ?: true,
            refreshInterval = prefs[SettingsKeys.REFRESH_INTERVAL] ?: 5,
            cpuAlertThreshold = prefs[SettingsKeys.CPU_ALERT] ?: 80,
            diskAlertThreshold = prefs[SettingsKeys.DISK_ALERT] ?: 90,
            backgroundMonitoringEnabled = prefs[SettingsKeys.BG_MONITORING] ?: false,
            backgroundMonitoringInterval = prefs[SettingsKeys.BG_INTERVAL] ?: 15,
            onboardingDone = prefs[SettingsKeys.ONBOARDING_DONE] ?: false,
            webhookType = prefs[SettingsKeys.WEBHOOK_TYPE] ?: "none",
            webhookUrl = prefs[SettingsKeys.WEBHOOK_URL] ?: "",
            telegramBotToken = prefs[SettingsKeys.TELEGRAM_BOT_TOKEN] ?: "",
            telegramChatId = prefs[SettingsKeys.TELEGRAM_CHAT_ID] ?: "",
            alertNotificationsEnabled = prefs[SettingsKeys.ALERT_NOTIFICATIONS] ?: true,
            biometricLockEnabled = prefs[SettingsKeys.BIOMETRIC_LOCK] ?: false,
            vpnDetectionEnabled = prefs[SettingsKeys.VPN_DETECTION] ?: false,
            profileDisplayName = prefs[SettingsKeys.PROFILE_DISPLAY_NAME] ?: "Admin"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val webhookType: StateFlow<String> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.WEBHOOK_TYPE] ?: "none"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")

    val webhookUrl: StateFlow<String> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.WEBHOOK_URL] ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val telegramBotToken: StateFlow<String> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.TELEGRAM_BOT_TOKEN] ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val telegramChatId: StateFlow<String> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.TELEGRAM_CHAT_ID] ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val darkTheme: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.DARK_THEME] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDarkTheme(enabled: Boolean) = save { it[SettingsKeys.DARK_THEME] = enabled }
    fun setRefreshInterval(secs: Int) = save { it[SettingsKeys.REFRESH_INTERVAL] = secs }
    fun setCpuAlertThreshold(value: Int) = save { it[SettingsKeys.CPU_ALERT] = value }
    fun setDiskAlertThreshold(value: Int) = save { it[SettingsKeys.DISK_ALERT] = value }
    fun setBackgroundMonitoringEnabled(enabled: Boolean) = save { it[SettingsKeys.BG_MONITORING] = enabled }
    fun setBackgroundMonitoringInterval(minutes: Int) = save { it[SettingsKeys.BG_INTERVAL] = minutes }
    fun setOnboardingDone(done: Boolean) = save { it[SettingsKeys.ONBOARDING_DONE] = done }
    fun setAlertNotificationsEnabled(enabled: Boolean) = save { it[SettingsKeys.ALERT_NOTIFICATIONS] = enabled }
    fun setBiometricLockEnabled(enabled: Boolean) = save { it[SettingsKeys.BIOMETRIC_LOCK] = enabled }
    fun setVpnDetectionEnabled(enabled: Boolean) = save { it[SettingsKeys.VPN_DETECTION] = enabled }
    fun setProfileDisplayName(name: String) = save { it[SettingsKeys.PROFILE_DISPLAY_NAME] = name }
    fun setWebhookType(type: String) = save { it[SettingsKeys.WEBHOOK_TYPE] = type }
    fun setWebhookUrl(url: String) = save { it[SettingsKeys.WEBHOOK_URL] = url }
    fun setTelegramBotToken(token: String) = save { it[SettingsKeys.TELEGRAM_BOT_TOKEN] = token }
    fun setTelegramChatId(chatId: String) = save { it[SettingsKeys.TELEGRAM_CHAT_ID] = chatId }

    fun sendTestWebhook() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val type = prefs[SettingsKeys.WEBHOOK_TYPE] ?: "none"
            val url = prefs[SettingsKeys.WEBHOOK_URL] ?: ""
            val token = prefs[SettingsKeys.TELEGRAM_BOT_TOKEN] ?: ""
            val chatId = prefs[SettingsKeys.TELEGRAM_CHAT_ID] ?: ""
            try {
                when (type) {
                    "slack" -> webhookService.sendSlackAlert(url, "Test alert from ServerControl", "Test Server")
                    "discord" -> webhookService.sendDiscordAlert(url, "Test alert from ServerControl", "Test Server")
                    "telegram" -> webhookService.sendTelegramAlert(token, chatId, "Test alert from ServerControl", "Test Server")
                }
            } catch (_: Exception) {}
        }
    }

    private fun save(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { dataStore.edit(block) }
    }
}
