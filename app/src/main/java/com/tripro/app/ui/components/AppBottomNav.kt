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
import androidx.compose.ui.res.stringResource
import com.tripro.app.R
import com.tripro.app.navigation.Destinations

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
            label = { Text(stringResource(R.string.nav_trips)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onSearchClick,
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_search)) }
        )
        NavigationBarItem(
            selected = currentRoute == Destinations.ALERTS,
            onClick = onAlertsClick,
            icon = {
                BadgedBox(badge = { if (hasUnreadAlerts) Badge() }) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                }
            },
            label = { Text(stringResource(R.string.nav_alerts)) }
        )
        NavigationBarItem(
            selected = currentRoute == Destinations.PROFILE,
            onClick = onProfileClick,
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_profile)) }
        )
    }
}