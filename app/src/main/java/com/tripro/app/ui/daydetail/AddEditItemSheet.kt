package com.tripro.app.ui.daydetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tripro.app.R
import com.tripro.app.data.model.*
import com.tripro.app.ui.components.SimpleTimePickerDialog
import com.tripro.app.ui.components.TriProTextField
import com.tripro.app.ui.theme.*
import com.tripro.app.util.PlaceSearchMapDialog
import com.tripro.app.util.localizedLabel
import com.tripro.app.ui.rememberAppContainer
import kotlinx.coroutines.launch

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
    var noteType by remember { mutableStateOf(existing?.noteType ?: NoteType.ALERT) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showLocationSearch by remember { mutableStateOf(false) }

    // Specialized fields for Flight/Hotel alignment
    val flightInfo = existing?.flightInfo
    var airline by remember { mutableStateOf(flightInfo?.airline.orEmpty()) }
    var flightNumber by remember { mutableStateOf(flightInfo?.flightNumber.orEmpty()) }
    var depCode by remember { mutableStateOf(flightInfo?.departureAirportCode.orEmpty()) }
    var arrCode by remember { mutableStateOf(flightInfo?.arrivalAirportCode.orEmpty()) }
    var depTime by remember { mutableStateOf(flightInfo?.departureTime.orEmpty()) }
    var arrTime by remember { mutableStateOf(flightInfo?.arrivalTime.orEmpty()) }
    var depLat by remember { mutableStateOf(flightInfo?.departureAirportLat) }
    var depLng by remember { mutableStateOf(flightInfo?.departureAirportLng) }
    var arrLat by remember { mutableStateOf(flightInfo?.arrivalAirportLat) }
    var arrLng by remember { mutableStateOf(flightInfo?.arrivalAirportLng) }
    var showDepPicker by remember { mutableStateOf(false) }
    var showArrPicker by remember { mutableStateOf(false) }
    var showDepSearch by remember { mutableStateOf(false) }
    var showArrSearch by remember { mutableStateOf(false) }

    var flightLookupQuery by remember { mutableStateOf(flightNumber.replace(" ", "")) }
    var isLookingUp by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    val hotelInfo = existing?.hotelInfo
    var checkIn by remember { mutableStateOf(hotelInfo?.checkIn.orEmpty()) }
    var checkOut by remember { mutableStateOf(hotelInfo?.checkOut.orEmpty()) }
    var hotelArrival by remember { mutableStateOf(hotelInfo?.arrivalTime.orEmpty()) }
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }
    var showHotelArrivalPicker by remember { mutableStateOf(false) }

    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()

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

            TriProTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.item_sheet_title_label),
                modifier = Modifier.fillMaxWidth()
            )

            if (type == ItemType.FLIGHT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    TriProTextField(
                        value = flightLookupQuery,
                        onValueChange = { flightLookupQuery = it.uppercase() },
                        label = stringResource(R.string.day_detail_flight_number_label),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val q = flightLookupQuery.trim()
                            val d = selectedDate ?: ""
                            if (q.isNotBlank() && d.isNotBlank() && !isLookingUp) {
                                isLookingUp = true
                                lookupError = null
                                scope.launch {
                                    container.flightLookupRepository.lookup(q, d)
                                        .onSuccess { r ->
                                            airline = r.airline; flightNumber = r.flightNumber
                                            depCode = r.departureAirportCode; arrCode = r.arrivalAirportCode
                                            depLat = r.departureAirportLat; depLng = r.departureAirportLng
                                            arrLat = r.arrivalAirportLat; arrLng = r.arrivalAirportLng
                                            depTime = r.departureTime; arrTime = r.arrivalTime
                                            if (title.isBlank()) title = "${r.airline} ${r.flightNumber}"
                                        }
                                        .onFailure { e ->
                                            lookupError = e.message ?: "Lookup failed"
                                        }
                                    isLookingUp = false
                                }
                            } else if (d.isBlank()) {
                                lookupError = "No date selected"
                            }
                        },
                        enabled = !isLookingUp && flightLookupQuery.isNotBlank(),
                        shape = TriProShapes.medium,
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isLookingUp) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.day_detail_look_up))
                    }
                }
                if (lookupError != null) {
                    Text(lookupError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TriProTextField(value = airline, onValueChange = { airline = it }, label = stringResource(R.string.day_detail_airline_label), modifier = Modifier.weight(1f))
                    TriProTextField(value = flightNumber, onValueChange = { flightNumber = it }, label = stringResource(R.string.day_detail_flight_number_short_label), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TriProTextField(value = depCode, onValueChange = { depCode = it.uppercase() }, label = stringResource(R.string.day_detail_from_code_label), modifier = Modifier.weight(1f))
                    TriProTextField(value = arrCode, onValueChange = { arrCode = it.uppercase() }, label = stringResource(R.string.day_detail_to_code_label), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDepSearch = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(if (depLat == null) stringResource(R.string.day_detail_find_departure_airport) else stringResource(R.string.day_detail_departure_confirmed), maxLines = 1)
                    }
                    OutlinedButton(onClick = { showArrSearch = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(if (arrLat == null) stringResource(R.string.day_detail_find_arrival_airport) else stringResource(R.string.day_detail_arrival_confirmed), maxLines = 1)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDepPicker = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) { Text(if (depTime.isBlank()) stringResource(R.string.day_detail_departs_label) else stringResource(R.string.day_detail_departs_prefix, depTime)) }
                    OutlinedButton(onClick = { showArrPicker = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) { Text(if (arrTime.isBlank()) stringResource(R.string.day_detail_arrives_label) else stringResource(R.string.day_detail_arrives_prefix, arrTime)) }
                }
            }

            if (type == ItemType.HOTEL) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showCheckInPicker = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) { Text(if (checkIn.isBlank()) stringResource(R.string.day_detail_checkin_time_label) else stringResource(R.string.day_detail_checkin_prefix, checkIn)) }
                    OutlinedButton(onClick = { showCheckOutPicker = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) { Text(if (checkOut.isBlank()) stringResource(R.string.day_detail_checkout_time_label) else stringResource(R.string.day_detail_checkout_prefix, checkOut)) }
                }
                OutlinedButton(onClick = { showHotelArrivalPicker = true }, modifier = Modifier.fillMaxWidth(), shape = TriProShapes.medium) { Text(if (hotelArrival.isBlank()) stringResource(R.string.day_detail_arrival_time_label) else stringResource(R.string.day_detail_arrival_prefix, hotelArrival)) }
            }

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
                TriProTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = stringResource(R.string.item_sheet_custom_what_label),
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
                TimeType.EXACT -> OutlinedButton(onClick = { showStartTimePicker = true }, shape = TriProShapes.medium) { Text(stringResource(R.string.item_sheet_start_prefix, startTime)) }
                TimeType.RANGE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) { Text(stringResource(R.string.item_sheet_from_prefix, startTime)) }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f), shape = TriProShapes.medium) { Text(stringResource(R.string.item_sheet_to_prefix, endTime)) }
                }
            }

            Text(stringResource(R.string.item_sheet_location_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            OutlinedButton(onClick = { showLocationSearch = true }, modifier = Modifier.fillMaxWidth(), shape = TriProShapes.medium) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(if (locationName.isBlank()) stringResource(R.string.item_sheet_search_maps) else stringResource(R.string.item_sheet_change_location))
            }
            TriProTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = stringResource(R.string.item_sheet_place_name_label),
                modifier = Modifier.fillMaxWidth()
            )
            TriProTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.item_sheet_address_optional_label),
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

            TriProTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.item_sheet_note_label),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                singleLine = false
            )

            Text(stringResource(R.string.item_sheet_note_type_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = noteType == NoteType.ALERT,
                    onClick = { noteType = NoteType.ALERT },
                    label = { Text(stringResource(R.string.item_sheet_note_type_alert)) }
                )
                FilterChip(
                    selected = noteType == NoteType.NOTE,
                    onClick = { noteType = NoteType.NOTE },
                    label = { Text(stringResource(R.string.item_sheet_note_type_info)) }
                )
            }

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
                            note = note,
                            noteType = noteType,
                            flightInfo = if (type == ItemType.FLIGHT) com.tripro.app.data.model.FlightInfo(
                                airline = airline, flightNumber = flightNumber,
                                departureAirportCode = depCode, arrivalAirportCode = arrCode,
                                departureAirportLat = depLat, departureAirportLng = depLng,
                                arrivalAirportLat = arrLat, arrivalAirportLng = arrLng,
                                departureTime = depTime, arrivalTime = arrTime
                            ) else null,
                            hotelInfo = if (type == ItemType.HOTEL) com.tripro.app.data.model.HotelInfo(
                                name = title, address = address, lat = pin?.latitude, lng = pin?.longitude,
                                checkIn = checkIn, checkOut = checkOut, arrivalTime = hotelArrival
                            ) else null
                        )
                    )
                },
                enabled = title.isNotBlank() && (dateOptions.isEmpty() || selectedDate != null),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TriProColors.Primary,
                    contentColor = TriProColors.OnPrimary
                )
            ) {
                Text(stringResource(R.string.action_save), style = TriProTypography.labelMedium)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_cancel), color = TriProColors.OnSurfaceVariant, style = TriProTypography.labelMedium)
            }
        }
    }

    if (showDepPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_departure_time_title), depTime.ifBlank { "09:00" }, { showDepPicker = false }, { depTime = it; showDepPicker = false })
    if (showArrPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_arrival_time_title), arrTime.ifBlank { "11:00" }, { showArrPicker = false }, { arrTime = it; showArrPicker = false })
    PlaceSearchMapDialog(visible = showDepSearch, typesFilter = listOf("airport"), onDismiss = { showDepSearch = false }, onPlacePicked = { p -> depLat = p.lat; depLng = p.lng; showDepSearch = false })
    PlaceSearchMapDialog(visible = showArrSearch, typesFilter = listOf("airport"), onDismiss = { showArrSearch = false }, onPlacePicked = { p -> arrLat = p.lat; arrLng = p.lng; showArrSearch = false })

    if (showCheckInPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_checkin_time_label), checkIn.ifBlank { "15:00" }, { showCheckInPicker = false }, { checkIn = it; showCheckInPicker = false })
    if (showCheckOutPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_checkout_time_label), checkOut.ifBlank { "11:00" }, { showCheckOutPicker = false }, { checkOut = it; showCheckOutPicker = false })
    if (showHotelArrivalPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_arrival_time_label), hotelArrival.ifBlank { "14:00" }, { showHotelArrivalPicker = false }, { hotelArrival = it; showHotelArrivalPicker = false })

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
