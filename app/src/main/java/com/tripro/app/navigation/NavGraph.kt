package com.tripro.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.tripro.app.ui.triplist.CreateTripRoute
import com.tripro.app.ui.triplist.TripsListRoute
import com.tripro.app.ui.tripoverview.TripOverviewRoute

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

            // Fires once per non-null deep link (e.g. a tapped push notification): jump
            // straight to the trip, or the specific day if the notification named one.
            LaunchedEffect(pendingDeepLink) {
                val link = pendingDeepLink ?: return@LaunchedEffect
                val route = if (link.date != null) {
                    Destinations.dayDetail(link.tripId, link.date)
                } else {
                    Destinations.tripOverview(link.tripId)
                }
                navController.navigate(route) { launchSingleTop = true }
                onDeepLinkConsumed()
            }

            NavHost(navController = navController, startDestination = Destinations.TRIPS_LIST) {
                composable(Destinations.TRIPS_LIST) {
                    TripsListRoute(
                        currentUid = uid,
                        onOpenTrip = { tripId -> navController.navigate(Destinations.tripOverview(tripId)) },
                        onCreateTrip = { navController.navigate(Destinations.CREATE_TRIP) }
                    )
                }
                composable(Destinations.CREATE_TRIP) {
                    CreateTripRoute(
                        ownerId = uid,
                        ownerName = displayName,
                        onTripCreated = { tripId ->
                            navController.navigate(Destinations.tripOverview(tripId)) {
                                popUpTo(Destinations.TRIPS_LIST)
                            }
                        },
                        onBack = { navController.popBackStack() }
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
                        onOpenCollaborators = { navController.navigate(Destinations.collaborators(tripId)) }
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
                    DayDetailRoute(
                        tripId = tripId,
                        date = date,
                        currentUid = uid,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Destinations.COLLABORATORS,
                    arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                ) { backStackEntry ->
                    val tripId = backStackEntry.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                    CollaboratorsRoute(
                        tripId = tripId,
                        currentUid = uid,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        else -> {
            LoginScreen(uiState = state, onSignInClick = { authViewModel.signIn() })
        }
    }
}
