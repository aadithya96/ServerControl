package com.servercontrol

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.servercontrol.presentation.navigation.NavGraph
import com.servercontrol.presentation.settings.SettingsKeys
import com.servercontrol.presentation.settings.SettingsViewModel
import com.servercontrol.presentation.theme.ServerControlTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var dataStore: DataStore<Preferences>

    private lateinit var isAuthenticatedState: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { false }
        enableEdgeToEdge()

        // Read biometric setting before the UI starts to avoid the race where
        // stateIn's default SettingsUiState(biometricLockEnabled=false) briefly
        // unlocks the app before DataStore loads the real value.
        val biometricEnabled = runBlocking {
            dataStore.data.first()[SettingsKeys.BIOMETRIC_LOCK] ?: false
        }
        isAuthenticatedState = mutableStateOf(!biometricEnabled)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val darkTheme by settingsViewModel.darkTheme.collectAsState()
            val settings by settingsViewModel.uiState.collectAsState()
            val isAuthenticated by isAuthenticatedState

            // Prompt whenever lock is enabled and the user isn't yet authenticated.
            // This also fires when the user toggles the setting ON mid-session.
            LaunchedEffect(settings.biometricLockEnabled, isAuthenticated) {
                if (settings.biometricLockEnabled && !isAuthenticated) {
                    showBiometricPrompt()
                }
            }

            // Always call rememberVpnState unconditionally (Compose rule: no conditional
            // composable calls). Gate the result on the user's preference instead.
            val vpnState = rememberVpnState()
            val vpnActive = settings.vpnDetectionEnabled && vpnState

            ServerControlTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!settings.biometricLockEnabled || isAuthenticated) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (vpnActive) {
                                VpnWarningBanner()
                            }
                            // When the banner occupies the status-bar area, consume that
                            // inset so the NavGraph screens below don't pad for it twice.
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (vpnActive) Modifier.consumeWindowInsets(WindowInsets.statusBars)
                                        else Modifier
                                    )
                            ) {
                                NavGraph()
                            }
                        }
                    } else {
                        BiometricLockScreen(onAuthenticate = { showBiometricPrompt() })
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock when the app leaves the foreground so the next open requires auth.
        val biometricEnabled = runBlocking {
            dataStore.data.first()[SettingsKeys.BIOMETRIC_LOCK] ?: false
        }
        if (biometricEnabled) {
            isAuthenticatedState.value = false
        }
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)

        // Device-credential (PIN/pattern/password) fallback is only safe to combine
        // with biometrics from API 30+. On older devices fall back to biometric-only.
        val allowDeviceCredential = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val combined = Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL

        val useCombined = allowDeviceCredential &&
            biometricManager.canAuthenticate(combined) == BiometricManager.BIOMETRIC_SUCCESS
        val canBiometric =
            biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

        // No biometric AND no device credential available: don't lock the user out
        // of their own app forever — just unlock.
        if (!useCombined && !canBiometric) {
            isAuthenticatedState.value = true
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isAuthenticatedState.value = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Keep locked — user dismissed or cancelled.
                isAuthenticatedState.value = false
            }
        })

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ServerControl")
            .setSubtitle("Verify your identity to access your servers")

        if (useCombined) {
            // A negative button cannot be combined with DEVICE_CREDENTIAL.
            builder.setAllowedAuthenticators(combined)
        } else {
            builder.setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK)
            builder.setNegativeButtonText("Cancel")
        }

        prompt.authenticate(builder.build())
    }
}

@Composable
private fun rememberVpnState(): Boolean {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(ConnectivityManager::class.java)
    }
    var vpnActive by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        fun check() {
            val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            vpnActive = caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        }
        check()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = check()
            override fun onLost(network: Network) = check()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = check()
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    return vpnActive
}

@Composable
private fun VpnWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF57F17))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            "VPN active — server connections may be affected",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun BiometricLockScreen(onAuthenticate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "ServerControl is locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Authenticate to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAuthenticate, modifier = Modifier.fillMaxWidth()) {
                Text("Unlock")
            }
        }
    }
}
