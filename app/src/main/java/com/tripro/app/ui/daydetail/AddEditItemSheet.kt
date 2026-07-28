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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TimeInput
import com.tripro.app.ui.components.PlaceSearchField
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.TimeType
import com.tripro.app.ui.theme.TriProSpacing

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
    var timeType by remember { mutableStateOf(existing?.timeType ?: TimeType.PERIOD) }
    var period by remember { mutableStateOf(existing?.period ?: DayPeriod.MORNING) }
    var startTime by remember { mutableStateOf(existing?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(existing?.endTime ?: "11:00") }
    var customTypeName by remember { mutableStateOf(existing?.customTypeName.orEmpty()) }
    var locationName by remember { mutableStateOf(existing?.locationName.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var pin by remember {
        mutableStateOf(
            if (existing?.lat != null && existing.lng != null) LatLng(existing.lat, existing.lng) else null
        )
    }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(TriProSpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            Text(
                if (existing == null) "Add to itinerary" else "Edit item",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text("When", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = timeType == TimeType.PERIOD, onClick = { timeType = TimeType.PERIOD }, label = { Text("Time of day") })
                FilterChip(selected = timeType == TimeType.EXACT, onClick = { timeType = TimeType.EXACT }, label = { Text("Exact time") })
                FilterChip(selected = timeType == TimeType.RANGE, onClick = { timeType = TimeType.RANGE }, label = { Text("Time range") })
            }

            when (timeType) {
                TimeType.PERIOD -> Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayPeriod.entries.forEach { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                    }
                }
                TimeType.EXACT -> OutlinedButton(onClick = { showStartTimePicker = true }) { Text("Start: $startTime") }
                TimeType.RANGE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }) { Text("From: $startTime") }
                    OutlinedButton(onClick = { showEndTimePicker = true }) { Text("To: $endTime") }
                }
            }

            if (type == ItemType.CUSTOM) {
                OutlinedTextField(
                    value = customTypeName,
                    onValueChange = { customTypeName = it },
                    label = { Text("What is it? (e.g. Scuba Diving)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PlaceSearchField(
                label = "Search Place",
                initialValue = locationName,
                onPlaceSelected = { result ->
                    locationName = result.name
                    address = result.address
                    pin = result.latLng
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Tap the map to drop a pin for this place (optional, powers the map view)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(pin ?: defaultMapCenter, 13f)
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng -> pin = latLng }
            ) {
                pin?.let { Marker(state = MarkerState(position = it)) }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note or alert (e.g. \"18+ only\", \"closes early at 18:00\")") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    onSave(
                        (existing ?: ItineraryItem()).copy(
                            title = title,
                            type = type,
                            timeType = timeType,
                            startTime = if (timeType != TimeType.PERIOD) startTime else null,
                            endTime = if (timeType == TimeType.RANGE) endTime else null,
                            period = if (timeType == TimeType.PERIOD) period else null,
                            customTypeName = if (type == ItemType.CUSTOM) customTypeName else "",
                            locationName = locationName,
                            address = address,
                            lat = pin?.latitude,
                            lng = pin?.longitude,
                            note = note
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialogSimple(
            initial = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { startTime = it; showStartTimePicker = false }
        )
    }
    if (showEndTimePicker) {
        TimePickerDialogSimple(
            initial = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { endTime = it; showEndTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogSimple(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initial.split(":")
    val state = rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9,
        initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimeInput(state = state) }
    )
}
