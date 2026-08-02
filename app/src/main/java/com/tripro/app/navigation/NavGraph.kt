package com.tripro.app.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.tripro.app.ui.settings.SettingsRoute
import com.tripro.app.ui.triplist.CreateTripRoute
import com.tripro.app.ui.triplist.TripsListRoute
import com.tripro.app.ui.tripoverview.TripDocsRoute
import com.tripro.app.ui.tripoverview.TripOverviewRoute
import com.tripro.app.util.ShareUtils
import kotlinx.coroutines.launch

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
            val context = LocalContext.current
            val app = context.applicationContext as TriProApplication
            val container = app.container

            // Created once for the whole signed-in session (per their own doc comments),
            // not scoped to a single NavBackStackEntry — that's what makes the bottom-nav
            // "unread alerts" badge and the Search popup instant everywhere.
            val alertsViewModel: AlertsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val prefs = context.getSharedPreferences("tripro_alerts", Context.MODE_PRIVATE)
                        AlertsViewModel(container.activityRepository, uid, prefs)
                    }
                }
            )
            val searchViewModel: SearchTripsViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { SearchTripsViewModel(container.tripRepository, uid) }
                }
            )
            val alertsUiState by alertsViewModel.uiState.collectAsState()

            LaunchedEffect(pendingDeepLink) {
                val link = pendingDeepLink ?: return@LaunchedEffect
                val route = if (link.date != null) Destinations.dayDetail(link.tripId, link.date) else Destinations.tripOverview(link.tripId)
                navController.navigate(route) { launchSingleTop = true }
                onDeepLinkConsumed()
            }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val drawerScope = rememberCoroutineScope()
            var showSearchDialog by remember { mutableStateOf(false) }

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val topLevelRoutes = setOf(Destinations.TRIPS_LIST, Destinations.ALERTS, Destinations.PROFILE)
            val showBottomBar = currentRoute in topLevelRoutes

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawerContent(
                        userName = displayName,
                        userEmail = state.user.email.orEmpty(),
                        userPhotoUrl = state.user.photoUrl?.toString().orEmpty(),
                        onNavigate = { route ->
                            drawerScope.launch { drawerState.close() }
                            navController.navigate(route) { launchSingleTop = true }
                        },
                        onInviteFriends = {
                            drawerScope.launch { drawerState.close() }
                            ShareUtils.shareAppInvite(context)
                        },
                        onSignOut = {
                            drawerScope.launch { drawerState.close() }
                            authViewModel.signOut()
                        }
                    )
                }
            ) {
                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            AppBottomNav(
                                currentRoute = currentRoute,
                                hasUnreadAlerts = alertsUiState.hasUnread,
                                onTripsClick = {
                                    navController.navigate(Destinations.tripsList()) {
                                        popUpTo(Destinations.TRIPS_LIST) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onSearchClick = { showSearchDialog = true },
                                onAlertsClick = { navController.navigate(Destinations.ALERTS) { launchSingleTop = true } },
                                onProfileClick = { navController.navigate(Destinations.PROFILE) { launchSingleTop = true } }
                            )
                        }
                    }
                ) { outerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Destinations.TRIPS_LIST,
                        modifier = Modifier.padding(outerPadding)
                    ) {
                        composable(
                            route = Destinations.TRIPS_LIST,
                            arguments = listOf(navArgument(Destinations.ARG_FILTER) {
                                type = NavType.StringType; nullable = true; defaultValue = null
                            })
                        ) {
                            TripsListRoute(
                                currentUid = uid,
                                onOpenTrip = { tripId -> navController.navigate(Destinations.tripOverview(tripId)) },
                                onCreateTrip = { navController.navigate(Destinations.CREATE_TRIP) },
                                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } }
                            )
                        }
                        composable(Destinations.ALERTS) {
                            AlertsRoute(
                                viewModel = alertsViewModel,
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                                onOpenTrip = { tripId, date ->
                                    val route = if (date != null) Destinations.dayDetail(tripId, date) else Destinations.tripOverview(tripId)
                                    navController.navigate(route)
                                }
                            )
                        }
                        composable(Destinations.PROFILE) {
                            ProfileRoute(
                                onOpenDrawer = { drawerScope.launch { drawerState.open() } },
                                onSignOut = { authViewModel.signOut() }
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
                        ) { backStackEntry ->
                            val tripId = backStackEntry.arguments?.getString(Destinations.ARG_TRIP_ID).orEmpty()
                            CollaboratorsRoute(
                                tripId = tripId,
                                currentUid = uid,
                                currentUserName = displayName,
                                onBack = { navController.popBackStack() }
                            )
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

                if (showSearchDialog) {
                    val trips by searchViewModel.trips.collectAsState()
                    SearchTripsDialog(
                        trips = trips,
                        onDismiss = { showSearchDialog = false },
                        onTripSelected = { tripId ->
                            showSearchDialog = false
                            navController.navigate(Destinations.tripOverview(tripId))
                        }
                    )
                }
            }
        }

        is AuthUiState.CheckingSession -> {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        else -> {
            LoginScreen(uiState = state, onSignInClick = { authViewModel.signIn() })
        }
    }
}