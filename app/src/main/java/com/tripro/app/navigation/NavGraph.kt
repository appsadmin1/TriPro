package com.tripro.app.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tripro.app.TriProApplication
import com.tripro.app.ui.alerts.AlertsRoute
import com.tripro.app.ui.alerts.AlertsViewModel
import com.tripro.app.ui.auth.AuthUiState
import com.tripro.app.ui.auth.AuthViewModel
import com.tripro.app.ui.auth.LoginScreen
import com.tripro.app.ui.collaborators.CollaboratorsRoute
import com.tripro.app.ui.components.AppBottomNav
import com.tripro.app.ui.daydetail.DayDetailRoute
import com.tripro.app.ui.navigation.AppDrawerContent
import com.tripro.app.ui.profile.ProfileRoute
import com.tripro.app.ui.search.SearchTripsDialog
import com.tripro.app.ui.search.SearchTripsViewModel
import com.tripro.app.ui.tripdocuments.TripDocumentsRoute
import com.tripro.app.ui.triplist.CreateTripRoute
import com.tripro.app.ui.triplist.TripsListRoute
import com.tripro.app.ui.tripoverview.TripOverviewRoute
import com.tripro.app.util.ShareUtils
import kotlinx.coroutines.launch

/** Bottom nav is only shown on these three top-level destinations — detail screens
 *  (trip overview, day detail, collaborators, documents, create-trip) keep it hidden so
 *  their own back-arrow TopAppBar reads as "I'm one level deep", matching the mockups. */
private val TOP_LEVEL_ROUTES = setOf(Destinations.TRIPS_LIST, Destinations.ALERTS, Destinations.PROFILE)

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
            val photoUrl = state.user.photoUrl?.toString().orEmpty()
            val email = state.user.email.orEmpty()

            val context = LocalContext.current
            val app = context.applicationContext as TriProApplication
            val container = app.container
            val prefs = LocalContext.current.getSharedPreferences("tripro_prefs", Context.MODE_PRIVATE)
            val scope = rememberCoroutineScope()
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            val alertsViewModel: AlertsViewModel = viewModel(
                factory = viewModelFactory { initializer { AlertsViewModel(container.activityRepository, uid, prefs) } }
            )
            val alertsState by alertsViewModel.uiState.collectAsState()

            val searchViewModel: SearchTripsViewModel = viewModel(
                factory = viewModelFactory { initializer { SearchTripsViewModel(container.tripRepository, uid) } }
            )
            val allTripsForSearch by searchViewModel.trips.collectAsState()
            var showSearch by remember { mutableStateOf(false) }

            LaunchedEffect(pendingDeepLink) {
                val link = pendingDeepLink ?: return@LaunchedEffect
                val route = if (link.date != null) Destinations.dayDetail(link.tripId, link.date) else Destinations.tripOverview(link.tripId)
                navController.navigate(route) { launchSingleTop = true }
                onDeepLinkConsumed()
            }

            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawerContent(
                        userName = displayName,
                        userEmail = email,
                        userPhotoUrl = photoUrl,
                        onNavigate = { route ->
                            scope.launch { drawerState.close() }
                            navController.navigate(route) {
                                popUpTo(Destinations.tripsList()) { inclusive = route == Destinations.tripsList() }
                                launchSingleTop = true
                            }
                        },
                        onInviteFriends = {
                            scope.launch { drawerState.close() }
                            ShareUtils.shareAppInvite(context)
                        },
                        onSignOut = {
                            scope.launch { drawerState.close() }
                            authViewModel.signOut()
                        }
                    )
                }
            ) {
                Scaffold(
                    bottomBar = {
                        if (currentRoute in TOP_LEVEL_ROUTES) {
                            AppBottomNav(
                                currentRoute = currentRoute,
                                hasUnreadAlerts = alertsState.hasUnread,
                                onTripsClick = { navController.navigate(Destinations.tripsList()) { launchSingleTop = true } },
                                onSearchClick = { showSearch = true },
                                onAlertsClick = { navController.navigate(Destinations.ALERTS) { launchSingleTop = true } },
                                onProfileClick = { navController.navigate(Destinations.PROFILE) { launchSingleTop = true } }
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Destinations.tripsList(),
                        modifier = Modifier.padding(padding)
                    ) {
                        composable(
                            route = Destinations.TRIPS_LIST,
                            arguments = listOf(navArgument(Destinations.ARG_FILTER) { type = NavType.StringType })
                        ) { backStack ->
                            val filter = backStack.arguments?.getString(Destinations.ARG_FILTER) ?: "all"
                            TripsListRoute(
                                currentUid = uid,
                                filter = filter,
                                onOpenTrip = { tripId -> navController.navigate(Destinations.tripOverview(tripId)) },
                                onCreateTrip = { navController.navigate(Destinations.CREATE_TRIP) },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }
                        composable(Destinations.CREATE_TRIP) {
                            CreateTripRoute(
                                ownerId = uid,
                                ownerName = displayName,
                                onTripCreated = { tripId ->
                                    navController.navigate(Destinations.tripOverview(tripId)) { popUpTo(Destinations.tripsList()) }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Destinations.TRIP_OVERVIEW,
                            arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                        ) { backStack ->
                            val tripId = backStack.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                            TripOverviewRoute(
                                tripId = tripId,
                                currentUid = uid,
                                onBack = { navController.popBackStack() },
                                onOpenDay = { date -> navController.navigate(Destinations.dayDetail(tripId, date)) },
                                onOpenCollaborators = { navController.navigate(Destinations.collaborators(tripId)) },
                                onOpenDocuments = { navController.navigate(Destinations.tripDocuments(tripId)) },
                                onTripDeleted = {
                                    navController.navigate(Destinations.tripsList()) { popUpTo(Destinations.tripsList()) { inclusive = true } }
                                }
                            )
                        }
                        composable(
                            route = Destinations.DAY_DETAIL,
                            arguments = listOf(
                                navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType },
                                navArgument(Destinations.ARG_DATE) { type = NavType.StringType }
                            )
                        ) { backStack ->
                            val tripId = backStack.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                            val date = backStack.arguments?.getString(Destinations.ARG_DATE).orEmpty()
                            DayDetailRoute(
                                tripId = tripId,
                                date = date,
                                currentUid = uid,
                                currentUserName = displayName,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Destinations.COLLABORATORS,
                            arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                        ) { backStack ->
                            val tripId = backStack.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                            CollaboratorsRoute(
                                tripId = tripId,
                                currentUid = uid,
                                currentUserName = displayName,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Destinations.TRIP_DOCUMENTS,
                            arguments = listOf(navArgument(Destinations.ARG_TRIP_ID) { type = NavType.StringType })
                        ) { backStack ->
                            val tripId = backStack.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                            TripDocumentsRoute(tripId = tripId, onBack = { navController.popBackStack() })
                        }
                        composable(Destinations.ALERTS) {
                            AlertsRoute(
                                viewModel = alertsViewModel,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onOpenTrip = { tripId, date ->
                                    val route = if (date != null) Destinations.dayDetail(tripId, date) else Destinations.tripOverview(tripId)
                                    navController.navigate(route)
                                }
                            )
                        }
                        composable(Destinations.PROFILE) {
                            ProfileRoute(
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }
                }
            }

            if (showSearch) {
                SearchTripsDialog(
                    trips = allTripsForSearch,
                    onDismiss = { showSearch = false },
                    onTripSelected = { tripId ->
                        showSearch = false
                        navController.navigate(Destinations.tripOverview(tripId))
                    }
                )
            }
        }

        else -> {
            LoginScreen(uiState = state, onSignInClick = { authViewModel.signIn() })
        }
    }
}