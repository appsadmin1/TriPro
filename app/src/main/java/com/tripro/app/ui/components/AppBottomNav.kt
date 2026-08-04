package com.tripro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tripro.app.R
import com.tripro.app.navigation.Destinations
import com.tripro.app.ui.theme.PillShape

import androidx.compose.foundation.layout.navigationBarsPadding

@Composable
fun AppBottomNav(
    currentRoute: String?,
    hasUnreadAlerts: Boolean,
    onTripsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // DESIGN.md: shadow-[0_-4px_20px_rgba(0,0,0,0.05)] rounded-t-xl h-20
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                NavTab(
                    route = Destinations.TRIPS_LIST,
                    icon = Icons.Filled.Explore,
                    label = stringResource(R.string.nav_trips),
                    onClick = onTripsClick
                ),
                NavTab(
                    route = "search", // Placeholder for now
                    icon = Icons.Filled.Search,
                    label = stringResource(R.string.nav_search),
                    onClick = onSearchClick
                ),
                NavTab(
                    route = Destinations.ALERTS,
                    icon = Icons.Filled.Notifications,
                    label = stringResource(R.string.nav_alerts),
                    onClick = onAlertsClick,
                    hasBadge = hasUnreadAlerts
                ),
                NavTab(
                    route = Destinations.PROFILE,
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.nav_profile),
                    onClick = onProfileClick
                )
            )

            navItems.forEach { tab ->
                val isSelected = currentRoute == tab.route
                BottomNavItem(tab = tab, isSelected = isSelected)
            }
        }
    }
}

private data class NavTab(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val hasBadge: Boolean = false
)

@Composable
private fun BottomNavItem(tab: NavTab, isSelected: Boolean) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = tab.onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BadgedBox(
                badge = {
                    if (tab.hasBadge) {
                        Badge(containerColor = MaterialTheme.colorScheme.error)
                    }
                }
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
