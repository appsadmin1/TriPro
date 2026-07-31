package com.tripro.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tripro.app.ui.auth.AuthUiState
import com.tripro.app.ui.auth.AuthViewModel
import com.tripro.app.ui.auth.LoginScreen
import com.tripro.app.ui.collaborators.CollaboratorsRoute
import com.tripro.app.ui.daydetail.DayDetailRoute
import com.tripro.app.ui.settings.SettingsRoute
import com.tripro.app.ui.triplist.CreateTripRoute
import com.tripro.app.ui.triplist.TripsListRoute
import com.tripro.app.ui.tripoverview.TripDocsRoute
import com.tripro.app.ui.tripoverview.TripOverviewRoute

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TriProNavGraph(
    authViewModel: AuthViewModel,
    pendingDeepLink: PendingDeepLink? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()

    when (val state = authState) {
        is AuthUiState.SignedIn -> {
            val uid = state.user.uid
            val displayName = state.user.displayName ?: state.user.email.orEmpty()

            LaunchedEffect(pendingDeepLink) {
                val link = pendingDeepLink ?: return@LaunchedEffect
                val route = if (link.date != null) Destinations.dayDetail(link.tripId, link.date) else Destinations.tripOverview(link.tripId)
                navController.navigate(route) { launchSingleTop = true }
                onDeepLinkConsumed()
            }

            NavHost(navController = navController, startDestination = Destinations.TRIPS_LIST) {
                composable(Destinations.TRIPS_LIST) {
                    TripsListRoute(
                        currentUid = uid,
                        onOpenTrip = { tripId -> navController.navigate(Destinations.tripOverview(tripId)) },
                        onCreateTrip = { navController.navigate(Destinations.CREATE_TRIP) },
                        onOpenSettings = { navController.navigate(Destinations.SETTINGS) }
                    )
                }
                composable(Destinations.CREATE_TRIP) {
                    CreateTripRoute(
                        ownerId = uid,
                        ownerName = displayName,
                        onTripCreated = { tripId -> navController.navigate(Destinations.tripOverview(tripId)) { popUpTo(Destinations.TRIPS_LIST) } },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Destinations.SETTINGS) {
                    SettingsRoute(
                        currentUid = uid,
                        onBack = { navController.popBackStack() },
                        onSignOut = { authViewModel.signOut() }
                    )
                }
                composable(
                    route = Destinations.TRIP_OVERVIEW,
                    arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                    TripOverviewRoute(
                        tripId = tripId,
                        currentUid = uid,
                        onBack = { navController.popBackStack() },
                        onOpenDay = { date -> navController.navigate(Destinations.dayDetail(tripId, date)) },
                        onOpenCollaborators = { navController.navigate(Destinations.collaborators(tripId)) },
                        onOpenDocs = { navController.navigate(Destinations.tripDocs(tripId)) },
                        onTripDeleted = { navController.popBackStack(Destinations.TRIPS_LIST, inclusive = false) }
                    )
                }
                composable(
                    route = Destinations.DAY_DETAIL,
                    arguments = listOf(
                        navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType },
                        navArgument(Destinations.ARG_DATE) { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                    val date = backStackEntry.arguments?.getString(Destinations.ARG_DATE).orEmpty()
                    DayDetailRoute(tripId = tripId, date = date, currentUid = uid, onBack = { navController.popBackStack() })
                }
                composable(
                    route = Destinations.COLLABORATORS,
                    arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                    CollaboratorsRoute(tripId = tripId, currentUid = uid, onBack = { navController.popBackStack() })
                }
                composable(
                    route = Destinations.TRIP_DOCS,
                    arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                    TripDocsRoute(tripId = tripId, onBack = { navController.popBackStack() })
                }
            }
        }

        is AuthUiState.CheckingSession -> {
            // Deliberately blank, not LoginScreen — see #12 below. The splash screen
            // covers this in practice, but this is the defense-in-depth fallback for the
            // one frame between the splash lifting and Firebase reporting a state.
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        else -> {
            LoginScreen(uiState = state, onSignInClick = { authViewModel.signIn() })
        }
    }
}