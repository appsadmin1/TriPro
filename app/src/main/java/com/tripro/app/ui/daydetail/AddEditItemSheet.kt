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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.tripro.app.R
import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.TimeType
import com.tripro.app.ui.components.SimpleTimePickerDialog
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.PlaceSearchMapDialog
import com.tripro.app.util.localizedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemSheet(
    existing: ItineraryItem?,
    defaultMapCenter: LatLng,
    onDismiss: () -> Unit,
    onSave: (ItineraryItem) -> Unit,
    dateOptions: List<Pair<String, String>> = emptyList(),
    selectedDate: String? = null,
    onDateSelected: ((String) -> Unit)? = null
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
        mutableStateOf(
            if (existing?.lat != null && existing.lng != null) LatLng(existing.lat, existing.lng) else null
        )
    }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
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
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(TriProSpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            Text(
                if (existing == null) stringResource(R.string.item_sheet_add_title) else stringResource(R.string.item_sheet_edit_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (dateOptions.isNotEmpty() && onDateSelected != null) {
                Text(stringResource(R.string.item_sheet_which_day), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateOptions.forEach { (date, label) ->
                        FilterChip(selected = date == selectedDate, onClick = { onDateSelected(date) }, label = { Text(label) })
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.item_sheet_title_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.item_sheet_type_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.localizedLabel()) }
                    )
                }
            }

            if (type == ItemType.CUSTOM) {
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text(stringResource(R.string.item_sheet_custom_what_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(stringResource(R.string.item_sheet_when_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = timeType == TimeType.PERIOD, onClick = { timeType = TimeType.PERIOD }, label = { Text(stringResource(R.string.item_sheet_time_of_day)) })
                FilterChip(selected = timeType == TimeType.EXACT, onClick = { timeType = TimeType.EXACT }, label = { Text(stringResource(R.string.item_sheet_exact_time)) })
                FilterChip(selected = timeType == TimeType.RANGE, onClick = { timeType = TimeType.RANGE }, label = { Text(stringResource(R.string.item_sheet_time_range)) })
            }

            when (timeType) {
                TimeType.PERIOD -> Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayPeriod.entries.forEach { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.localizedLabel()) })
                    }
                }
                TimeType.EXACT -> OutlinedButton(onClick = { showStartTimePicker = true }) { Text(stringResource(R.string.item_sheet_start_prefix, startTime)) }
                TimeType.RANGE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.item_sheet_from_prefix, startTime)) }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.item_sheet_to_prefix, endTime)) }
                }
            }

            Text(stringResource(R.string.item_sheet_location_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(onClick = { showLocationSearch = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (locationName.isBlank()) stringResource(R.string.item_sheet_search_maps) else stringResource(R.string.item_sheet_change_location))
            }
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text(stringResource(R.string.item_sheet_place_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.item_sheet_address_optional_label)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                stringResource(R.string.item_sheet_finetune_map),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                label = { Text(stringResource(R.string.item_sheet_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = {
                    onSave(
                        (existing ?: ItineraryItem()).copy(
                            title = title,
                            type = type,
                            customLabel = if (type == ItemType.CUSTOM) customLabel else "",
                            timeType = timeType,
                            startTime = if (timeType != TimeType.PERIOD) startTime else null,
                            endTime = if (timeType == TimeType.RANGE) endTime else null,
                            period = if (timeType == TimeType.PERIOD) period else null,
                            locationName = locationName,
                            address = address,
                            lat = pin?.latitude,
                            lng = pin?.longitude,
                            note = note
                        )
                    )
                },
                enabled = title.isNotBlank() && (dateOptions.isEmpty() || selectedDate != null),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }
        }
    }

    if (showStartTimePicker) {
        SimpleTimePickerDialog(
            title = stringResource(R.string.item_sheet_start_time_title),
            initial = startTime,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { startTime = it; showStartTimePicker = false }
        )
    }
    if (showEndTimePicker) {
        SimpleTimePickerDialog(
            title = stringResource(R.string.item_sheet_end_time_title),
            initial = endTime,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { endTime = it; showEndTimePicker = false }
        )
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