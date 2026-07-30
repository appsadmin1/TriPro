package com.tripro.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tripro.app.data.model.ActivityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsRoute(
    viewModel: AlertsViewModel,
    onOpenDrawer: () -> Unit,
    onOpenTrip: (tripId: String, date: String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Opening the tab is what clears the unread badge.
    LaunchedEffect(Unit) { viewModel.markAllSeen() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts") },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Filled.Menu, contentDescription = "Menu") } }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No updates yet — changes to your trips will show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.message) },
                        supportingContent = { Text("${entry.tripName} · ${entry.actorName}") },
                        leadingContent = { Icon(iconFor(entry.type), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .clickable { onOpenTrip(entry.tripId, entry.date) }
                    )
                }
            }
        }
    }
}

private fun iconFor(type: ActivityType): ImageVector = when (type) {
    ActivityType.ITEM_ADDED -> Icons.Filled.AddCircle
    ActivityType.ITEM_UPDATED -> Icons.Filled.Edit
    ActivityType.ITEM_REMOVED -> Icons.Filled.Delete
    ActivityType.HOTEL_UPDATED -> Icons.Filled.Hotel
    ActivityType.FLIGHT_UPDATED -> Icons.Filled.FlightTakeoff
    ActivityType.DAY_NOTE_UPDATED -> Icons.Filled.StickyNote2
    ActivityType.MEMBER_INVITED -> Icons.Filled.PersonAdd
    ActivityType.MEMBER_ROLE_CHANGED -> Icons.Filled.ManageAccounts
    ActivityType.MEMBER_REMOVED -> Icons.Filled.PersonRemove
}