package com.tripro.app.ui.triplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.ui.components.TripCard
import com.tripro.app.ui.theme.TriProSpacing

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FlightTakeoff

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsListRoute(
    currentUid: String,
    onOpenTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    // ...
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container

    val viewModel: TripsListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TripsListViewModel(container.tripRepository, container.userRepository, currentUid) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTrip,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(percent = 50) // DESIGN.md: Pill shape for indicators
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.trips_create_cd))
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = TriProSpacing.marginMobile,
                    end = TriProSpacing.marginMobile,
                    top = 24.dp,
                    bottom = 120.dp // Extra padding for bottom nav
                ),
                verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackLg)
            ) {
                item {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu_cd), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                stringResource(R.string.trips_title),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp), // Slightly smaller to fit next to icon
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            stringResource(R.string.trips_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 48.dp) // Align with title text
                        )
                    }
                }

                if (uiState.upcoming.isNotEmpty()) {
                    item {
                        SectionHeader(icon = Icons.Filled.FlightTakeoff, title = stringResource(R.string.trips_section_upcoming))
                    }
                    items(uiState.upcoming, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            collaboratorPhotoUrls = uiState.memberAvatars[trip.id].orEmpty(),
                            onClick = { onOpenTrip(trip.id) }
                        )
                    }
                } else {
                    item {
                        EmptyState()
                    }
                }

                if (uiState.past.isNotEmpty()) {
                    item {
                        SectionHeader(icon = Icons.Filled.History, title = stringResource(R.string.trips_section_past), isSecondary = true)
                    }
                    items(uiState.past, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            collaboratorPhotoUrls = uiState.memberAvatars[trip.id].orEmpty(),
                            onClick = { onOpenTrip(trip.id) },
                            isPast = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, isSecondary: Boolean = false) {
    val color = if (isSecondary) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = color,
            modifier = if (isRtl && icon == Icons.Filled.FlightTakeoff) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Text(
        stringResource(R.string.trips_empty_state),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}