package com.tripro.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tripro.app.AppContainer
import com.tripro.app.TriProApplication

/** Grabs the singleton AppContainer from the Application. Use inside a `viewModel { }`
 *  factory block so each screen wires its own ViewModel by hand — see any *Screen.kt
 *  entry point (e.g. TripsListRoute) for the pattern. */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext as TriProApplication
    return context.container
}
