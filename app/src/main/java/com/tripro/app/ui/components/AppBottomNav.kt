package com.tripro.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.tripro.app.navigation.Destinations

/** [currentRoute] is the NavBackStackEntry's route *pattern* (e.g. "trips/list/{filter}"),
 *  not a resolved path — that's what makes the equality checks below stable regardless
 *  of which filter/trip/day is actually open. */
@Composable
fun AppBottomNav(
    currentRoute: String?,
    hasUnreadAlerts: Boolean,
    onTripsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Destinations.TRIPS_LIST,
            onClick = onTripsClick,
            icon = { Icon(Icons.Filled.Explore, contentDescription = null) },
            label = { Text("Trips") }
        )
        NavigationBarItem(
            selected = false, // opens a popup, never "the current screen"
            onClick = onSearchClick,
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = currentRoute == Destinations.ALERTS,
            onClick = onAlertsClick,
            icon = {
                BadgedBox(badge = { if (hasUnreadAlerts) Badge() }) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                }
            },
            label = { Text("Alerts") }
        )
        NavigationBarItem(
            selected = currentRoute == Destinations.PROFILE,
            onClick = onProfileClick,
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Profile") }
        )
    }
}