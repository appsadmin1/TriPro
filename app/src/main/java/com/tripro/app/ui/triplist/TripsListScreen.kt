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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.tripro.app.TriProApplication
import com.tripro.app.ui.components.TripCard
import com.tripro.app.ui.theme.TriProSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsListRoute(
    currentUid: String,
    filter: String,
    onOpenTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container

    val viewModel: TripsListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TripsListViewModel(container.tripRepository, container.userRepository, currentUid) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    val (title, subtitle) = when (filter) {
        "upcoming" -> "Upcoming Trips" to "Everything still ahead of you."
        "past" -> "Old Trips" to "Everywhere you've already been."
        else -> "My Trips" to "Your upcoming and past adventures."
    }
    val showUpcoming = filter != "past"
    val showPast = filter != "upcoming"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTrip,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create new vacation")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TriProSpacing.marginMobile,
                end = TriProSpacing.marginMobile,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackLg)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column {
                    Text(title, style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp), color = MaterialTheme.colorScheme.primary)
                    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (showUpcoming) {
                if (uiState.upcoming.isNotEmpty()) {
                    item { SectionHeader(icon = Icons.Filled.Add, title = "Upcoming") }
                    items(uiState.upcoming, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            collaboratorPhotoUrls = uiState.memberAvatars[trip.id].orEmpty(),
                            onClick = { onOpenTrip(trip.id) }
                        )
                    }
                } else if (filter == "upcoming") {
                    item { EmptyState("No upcoming trips — tap the + button to plan your next one.") }
                }
            }

            if (showPast) {
                if (uiState.past.isNotEmpty()) {
                    item { SectionHeader(icon = Icons.Filled.History, title = "Past Adventures") }
                    items(uiState.past, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            collaboratorPhotoUrls = uiState.memberAvatars[trip.id].orEmpty(),
                            onClick = { onOpenTrip(trip.id) },
                            isPast = true
                        )
                    }
                } else if (filter == "past") {
                    item { EmptyState("No past trips yet.") }
                }
            }

            if (filter == "all" && uiState.upcoming.isEmpty() && uiState.past.isEmpty()) {
                item { EmptyState("No trips yet — tap the + button to plan your first one.") }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}