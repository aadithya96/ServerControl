package com.servercontrol.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
}

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val refreshInterval: Int = 5,
    val cpuAlertThreshold: Int = 80,
    val diskAlertThreshold: Int = 90,
    val backgroundMonitoringEnabled: Boolean = false,
    val backgroundMonitoringInterval: Int = 15,
    val onboardingDone: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = dataStore.data.map { prefs ->
        SettingsUiState(
            isDarkTheme = prefs[SettingsKeys.DARK_THEME] ?: true,
            refreshInterval = prefs[SettingsKeys.REFRESH_INTERVAL] ?: 5,
            cpuAlertThreshold = prefs[SettingsKeys.CPU_ALERT] ?: 80,
            diskAlertThreshold = prefs[SettingsKeys.DISK_ALERT] ?: 90,
            backgroundMonitoringEnabled = prefs[SettingsKeys.BG_MONITORING] ?: false,
            backgroundMonitoringInterval = prefs[SettingsKeys.BG_INTERVAL] ?: 15,
            onboardingDone = prefs[SettingsKeys.ONBOARDING_DONE] ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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

    private fun save(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { dataStore.edit(block) }
    }
}
