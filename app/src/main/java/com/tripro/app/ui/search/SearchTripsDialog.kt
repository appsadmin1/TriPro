package com.tripro.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tripro.app.data.model.Trip
import com.tripro.app.util.DateUtils

@Composable
fun SearchTripsDialog(
    trips: List<Trip>,
    onDismiss: () -> Unit,
    onTripSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, trips) {
        if (query.isBlank()) trips
        else trips.filter { it.name.contains(query, ignoreCase = true) || it.destination.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 4.dp) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(20.dp).fillMaxWidth().heightIn(max = 480.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search your trips") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
                if (filtered.isEmpty()) {
                    Text(
                        "No trips match \"$query\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(filtered, key = { it.id }) { trip ->
                            ListItem(
                                headlineContent = { Text(trip.name) },
                                supportingContent = { Text(DateUtils.formatRange(trip.startDate, trip.endDate)) },
                                leadingContent = { Icon(Icons.Filled.FlightTakeoff, contentDescription = null) },
                                modifier = Modifier.clickable { onTripSelected(trip.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}