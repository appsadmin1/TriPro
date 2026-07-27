package com.tripro.app

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.tripro.app.navigation.PendingDeepLink
import com.tripro.app.navigation.TriProNavGraph
import com.tripro.app.notifications.NotificationHelper
import com.tripro.app.ui.auth.AuthUiState
import com.tripro.app.ui.auth.AuthViewModel
import com.tripro.app.ui.theme.TriProTheme

class MainActivity : ComponentActivity() {

    // Held at the Activity level (not inside the composable) so onNewIntent can update it
    // directly — Compose state can be written from anywhere and still triggers recomposition
    // of whoever reads it with `by`, so this avoids recreating the whole Activity on a
    // notification tap while the app is already running.
    private val deepLinkState = mutableStateOf<PendingDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkState.value = deepLinkFrom(intent)
        val container = (application as TriProApplication).container

        setContent {
            TriProTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer { AuthViewModel(container.authRepository, container.userRepository) }
                        }
                    )
                    val authState by authViewModel.uiState.collectAsState()
                    val pendingDeepLink by deepLinkState

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* no-op either way — worst case, no pushes show up */ }

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
