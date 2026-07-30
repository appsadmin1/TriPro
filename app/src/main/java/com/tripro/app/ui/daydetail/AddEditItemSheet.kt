package com.tripro.app.ui.daydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.NoteType
import com.tripro.app.data.model.TimeType
import com.tripro.app.ui.components.SimpleTimePickerDialog
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.PlaceSearchMapDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemSheet(
    existing: ItineraryItem?,
    defaultMapCenter: LatLng,
    onDismiss: () -> Unit,
    onSave: (ItineraryItem) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var type by remember { mutableStateOf(existing?.type ?: ItemType.CUSTOM) }
    var customLabel by remember { mutableStateOf(existing?.customLabel.orEmpty()) }
    var timeType by remember { mutableStateOf(existing?.timeType ?: TimeType.PERIOD) }
    var period by remember { mutableStateOf(existing?.period ?: DayPeriod.MORNING) }
    var startTime by remember { mutableStateOf(existing?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(existing?.endTime ?: "11:00") }
    var locationName by remember { mutableStateOf(existing?.locationName.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var pin by remember {
        mutableStateOf(if (existing?.lat != null && existing.lng != null) LatLng(existing.lat, existing.lng) else null)
    }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var noteType by remember { mutableStateOf(existing?.noteType ?: NoteType.ALERT) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showLocationSearch by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pin ?: defaultMapCenter, 13f)
    }

    LaunchedEffect(pin) {
        pin?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(TriProSpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            Text(if (existing == null) "Add to itinerary" else "Edit item", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())

            Text("Type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemType.entries.forEach { option ->
                    FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }

            if (type == ItemType.CUSTOM) {
                OutlinedTextField(
                    value = customLabel, onValueChange = { customLabel = it },
                    label = { Text("What is this? (e.g. Grocery run, Laundry)") }, modifier = Modifier.fillMaxWidth()
                )
            }

            Text("When", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = timeType == TimeType.PERIOD, onClick = { timeType = TimeType.PERIOD }, label = { Text("Time of day") })
                FilterChip(selected = timeType == TimeType.EXACT, onClick = { timeType = TimeType.EXACT }, label = { Text("Exact time") })
                FilterChip(selected = timeType == TimeType.RANGE, onClick = { timeType = TimeType.RANGE }, label = { Text("Time range") })
            }

            when (timeType) {
                TimeType.PERIOD -> Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayPeriod.entries.forEach { p -> FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) }) }
                }
                TimeType.EXACT -> OutlinedButton(onClick = { showStartTimePicker = true }) { Text("Start: $startTime") }
                TimeType.RANGE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("From: $startTime") }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("To: $endTime") }
                }
            }

            Text("Location", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(onClick = { showLocationSearch = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (locationName.isBlank()) "Search on Google Maps" else "Change location")
            }
            OutlinedTextField(value = locationName, onValueChange = { locationName = it }, label = { Text("Place name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address (optional)") }, modifier = Modifier.fillMaxWidth())

            Text("Fine-tune by tapping the map", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng -> pin = latLng }
            ) {
                pin?.let { Marker(state = MarkerState(position = it)) }
            }

            // Item 3: alert (red/warning) vs. note (green/exclamation).
            Text("Note type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = noteType == NoteType.ALERT,
                    onClick = { noteType = NoteType.ALERT },
                    label = { Text("Alert") },
                    leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
                FilterChip(
                    selected = noteType == NoteType.NOTE,
                    onClick = { noteType = NoteType.NOTE },
                    label = { Text("Note") },
                    leadingIcon = { Icon(Icons.Filled.PriorityHigh, contentDescription = null, tint = HorizonEthosColors.Success) }
                )
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text(if (noteType == NoteType.ALERT) "Alert text (e.g. \"18+ only\", \"closes early at 18:00\")" else "Note text") },
                modifier = Modifier.fillMaxWidth(), minLines = 2
            )

            Button(
                onClick = {
                    onSave(
                        (existing ?: ItineraryItem()).copy(
                            title = title, type = type,
                            customLabel = if (type == ItemType.CUSTOM) customLabel else "",
                            timeType = timeType,
                            startTime = if (timeType != TimeType.PERIOD) startTime else null,
                            endTime = if (timeType == TimeType.RANGE) endTime else null,
                            period = if (timeType == TimeType.PERIOD) period else null,
                            locationName = locationName, address = address,
                            lat = pin?.latitude, lng = pin?.longitude,
                            note = note, noteType = noteType
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }

    if (showStartTimePicker) {
        SimpleTimePickerDialog(title = "Start time", initial = startTime, onDismiss = { showStartTimePicker = false }, onConfirm = { startTime = it; showStartTimePicker = false })
    }
    if (showEndTimePicker) {
        SimpleTimePickerDialog(title = "End time", initial = endTime, onDismiss = { showEndTimePicker = false }, onConfirm = { endTime = it; showEndTimePicker = false })
    }
    PlaceSearchMapDialog(
        visible = showLocationSearch,
        onDismiss = { showLocationSearch = false },
        onPlacePicked = { picked ->
            locationName = picked.name
            address = picked.address
            pin = LatLng(picked.lat, picked.lng)
            showLocationSearch = false
        }
    )
}