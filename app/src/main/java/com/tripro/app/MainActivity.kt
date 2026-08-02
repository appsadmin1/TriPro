package com.tripro.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tripro.app.navigation.PendingDeepLink
import com.tripro.app.navigation.TriProNavGraph
import com.tripro.app.notifications.NotificationHelper
import com.tripro.app.ui.auth.AuthUiState
import com.tripro.app.ui.auth.AuthViewModel
import com.tripro.app.ui.theme.TriProTheme
import androidx.compose.runtime.remember
import com.tripro.app.util.LanguagePreference
import com.tripro.app.util.applyAppLocale

class MainActivity : ComponentActivity() {

    private val deepLinkState = mutableStateOf<PendingDeepLink?>(null)
    private lateinit var authViewModel: AuthViewModel

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.applyAppLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkState.value = deepLinkFrom(intent)
        val container = (application as TriProApplication).container

        authViewModel = ViewModelProvider(
            this,
            viewModelFactory { initializer { AuthViewModel(container.authRepository, container.userRepository) } }
        )[AuthViewModel::class.java]

        // Keeps the OS splash on screen until we know whether there's a cached signed-in
        // user, so there's no frame where Compose could draw the login screen only to
        // immediately replace it with the trips list.
        splashScreen.setKeepOnScreenCondition { authViewModel.uiState.value is AuthUiState.CheckingSession }

        setContent {
            val appLanguage = remember { LanguagePreference.get(this@MainActivity) }
            TriProTheme(appLanguage = appLanguage) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val authState by authViewModel.uiState.collectAsState()
                    val pendingDeepLink by deepLinkState

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* no-op either way */ }

                    LaunchedEffect(authState) {
                        if (authState is AuthUiState.SignedIn &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    TriProNavGraph(
                        authViewModel = authViewModel,
                        pendingDeepLink = pendingDeepLink,
                        onDeepLinkConsumed = { deepLinkState.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkFrom(intent)?.let { deepLinkState.value = it }
    }

    private fun deepLinkFrom(intent: Intent?): PendingDeepLink? {
        val tripId = intent?.getStringExtra(NotificationHelper.EXTRA_TRIP_ID) ?: return null
        val date = intent.getStringExtra(NotificationHelper.EXTRA_DATE)
        return PendingDeepLink(tripId, date)
    }
}