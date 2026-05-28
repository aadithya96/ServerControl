package com.servercontrol.presentation.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    val DARK_THEME = booleanPreferencesKey("dark_theme")
    val REFRESH_INTERVAL = intPreferencesKey("refresh_interval_seconds")
    val CPU_ALERT_THRESHOLD = floatPreferencesKey("cpu_alert_threshold")
    val DISK_ALERT_THRESHOLD = floatPreferencesKey("disk_alert_threshold")
    val BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
}

data class SettingsUiState(
    val darkTheme: Boolean = true,
    val refreshIntervalSeconds: Int = 5,
    val cpuAlertThreshold: Float = 85f,
    val diskAlertThreshold: Float = 90f,
    val backgroundMonitoring: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = dataStore.data.map { prefs ->
        SettingsUiState(
            darkTheme = prefs[SettingsKeys.DARK_THEME] ?: true,
            refreshIntervalSeconds = prefs[SettingsKeys.REFRESH_INTERVAL] ?: 5,
            cpuAlertThreshold = prefs[SettingsKeys.CPU_ALERT_THRESHOLD] ?: 85f,
            diskAlertThreshold = prefs[SettingsKeys.DISK_ALERT_THRESHOLD] ?: 90f,
            backgroundMonitoring = prefs[SettingsKeys.BACKGROUND_MONITORING] ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    val darkTheme: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.DARK_THEME] ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setDarkTheme(enabled: Boolean) = save { it[SettingsKeys.DARK_THEME] = enabled }
    fun setRefreshInterval(secs: Int) = save { it[SettingsKeys.REFRESH_INTERVAL] = secs }
    fun setCpuAlertThreshold(value: Float) = save { it[SettingsKeys.CPU_ALERT_THRESHOLD] = value }
    fun setDiskAlertThreshold(value: Float) = save { it[SettingsKeys.DISK_ALERT_THRESHOLD] = value }
    fun setBackgroundMonitoring(enabled: Boolean) = save { it[SettingsKeys.BACKGROUND_MONITORING] = enabled }

    private fun save(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch { dataStore.edit(block) }
    }
}
