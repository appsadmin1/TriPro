package com.tripro.app.ui.daydetail

import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.google.android.gms.maps.model.LatLng
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.FlightInfo
import com.tripro.app.data.model.HotelInfo
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.WeatherStatus
import com.tripro.app.ui.components.AttachmentViewerDialog
import com.tripro.app.ui.components.DayMapPreview
import com.tripro.app.ui.components.ItineraryItemRow
import com.tripro.app.ui.components.MapPin
import com.tripro.app.ui.components.SimpleTimePickerDialog
import com.tripro.app.ui.components.WeatherCard
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.DateUtils
import com.tripro.app.util.PickedPlace
import com.tripro.app.util.PlaceSearchMapDialog
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.toMarkerColorKey
import com.tripro.app.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailRoute(
    tripId: String,
    date: String,
    currentUid: String,
    currentUserName: String,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: DayDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DayDetailViewModel(
                    container.tripRepository,
                    container.weatherRepository,
                    container.cloudinaryRepository,
                    container.pushNotificationRepository,
                    container.userRepository,
                    tripId, date, currentUid
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = LocalContext.current.contentResolver

    var isEditMode by remember { mutableStateOf(false) }
    val editingAllowed = uiState.canEdit && isEditMode

    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItineraryItem?>(null) }
    var showHotelDialog by remember { mutableStateOf(false) }
    var showFlightDialog by remember { mutableStateOf(false) }
    var showDayNoteDialog by remember { mutableStateOf(false) }
    var pendingAttachmentItemId by remember { mutableStateOf<String?>(null) }
    var viewingAttachment by remember { mutableStateOf<Pair<String, Attachment>?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    var pendingUpload by remember { mutableStateOf<Pair<String, Uri>?>(null) }
    var pendingUploadName by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val itemId = pendingAttachmentItemId
        if (uri != null && itemId != null) {
            pendingUploadName = queryFileName(contentResolver, uri) ?: "file"
            pendingUpload = itemId to uri
        }
        pendingAttachmentItemId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.day_detail_day_label, uiState.day?.dayIndex?.toString().orEmpty()), style = MaterialTheme.typography.bodySmall)
                        Text(DateUtils.formatFullDayLabel(date), style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd)) } },
                actions = {
                    if (uiState.canEdit) {
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(
                                if (isEditMode) Icons.Filled.Close else Icons.Filled.Edit,
                                contentDescription = if (isEditMode) stringResource(R.string.day_detail_done_editing_cd) else stringResource(R.string.day_detail_edit_day_cd),
                                tint = if (isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (editingAllowed) {
                FloatingActionButton(
                    onClick = { editingItem = null; showAddItemSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.day_detail_add_to_itinerary_cd)) }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val day = uiState.day
        val activityColors = uiState.activityColors
        val pins = buildList {
            day?.hotel?.let { h ->
                if (h.lat != null && h.lng != null) {
                    add(MapPin("Hotel: ${h.name}", h.address, h.lat, h.lng, colorInt = activityColors.colorInt(MarkerColorKey.HOTEL)))
                }
            }
            uiState.items.forEach { i ->
                if (i.lat != null && i.lng != null) {
                    add(MapPin(i.title, i.locationName, i.lat, i.lng, colorInt = activityColors.colorInt(i.type.toMarkerColorKey())))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TriProSpacing.marginMobile, end = TriProSpacing.marginMobile,
                top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            item { HotelCard(hotel = day?.hotel, canEdit = editingAllowed, onEdit = { showHotelDialog = true }) }

            if (day?.flight != null) {
                item { FlightCard(flight = day.flight, canEdit = editingAllowed, onEdit = { showFlightDialog = true }) }
            } else if (editingAllowed) {
                item { AddFlightButton(onClick = { showFlightDialog = true }) }
            }

            item {
                WeatherCard(
                    weather = uiState.weather, isLoading = uiState.weatherLoading,
                    forecastAvailableFromLabel = if (uiState.weather?.status == WeatherStatus.NOT_YET_AVAILABLE) viewModel.forecastAvailableFromLabel() else null
                )
            }

            if (pins.isNotEmpty()) {
                item { DayMapPreview(pins = pins) }
            }

            item { DayNoteCard(note = day?.dayNote.orEmpty(), canEdit = editingAllowed, onEdit = { showDayNoteDialog = true }) }

            item { Text(stringResource(R.string.day_detail_schedule), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }

            if (uiState.items.isEmpty()) {
                item { Text(stringResource(R.string.day_detail_nothing_planned), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            items(uiState.items, key = { it.id }) { item ->
                ItineraryItemRow(
                    item = item,
                    canEdit = editingAllowed,
                    onEdit = { editingItem = item; showAddItemSheet = true },
                    onDelete = { viewModel.deleteItem(item.id) },
                    onAddAttachment = { pendingAttachmentItemId = item.id; filePicker.launch(arrayOf("*/*")) },
                    onAttachmentClick = { attachment -> viewingAttachment = item.id to attachment }
                )
            }
        }
    }

    if (showAddItemSheet) {
        AddEditItemSheet(
            existing = editingItem,
            defaultMapCenter = mapCenterOrDefault(uiState.day?.hotel),
            onDismiss = { showAddItemSheet = false },
            onSave = { item ->
                if (editingItem == null) viewModel.addItem(item) else viewModel.updateItem(item)
                showAddItemSheet = false
            }
        )
    }

    if (showHotelDialog) {
        HotelEditDialog(existing = uiState.day?.hotel, onDismiss = { showHotelDialog = false }, onSave = { hotel -> viewModel.updateHotel(hotel); showHotelDialog = false })
    }

    if (showFlightDialog) {
        FlightEditDialog(existing = uiState.day?.flight, date = date, onDismiss = { showFlightDialog = false }, onSave = { flight -> viewModel.updateFlight(flight); showFlightDialog = false })
    }

    if (showDayNoteDialog) {
        DayNoteEditDialog(existing = uiState.day?.dayNote.orEmpty(), onDismiss = { showDayNoteDialog = false }, onSave = { note -> viewModel.updateDayNote(note); showDayNoteDialog = false })
    }

    viewingAttachment?.let { (itemId, attachment) ->
        AttachmentViewerDialog(
            attachment = attachment,
            onDismiss = { viewingAttachment = null },
            onRemove = if (uiState.canEdit) ({ viewModel.removeAttachment(itemId, attachment) }) else null,
            onRename = if (uiState.canEdit) ({ newName -> viewModel.renameAttachment(itemId, attachment, newName) }) else null
        )
    }

    pendingUpload?.let { (itemId, uri) ->
        AlertDialog(
            onDismissRequest = { pendingUpload = null },
            title = { Text(stringResource(R.string.day_detail_name_file_title)) },
            text = {
                OutlinedTextField(
                    value = pendingUploadName,
                    onValueChange = { pendingUploadName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.uploadAttachment(contentResolver, itemId, uri, pendingUploadName.ifBlank { "file" })
                    pendingUpload = null
                }) { Text(stringResource(R.string.action_upload)) }
            },
            dismissButton = { TextButton(onClick = { pendingUpload = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

private fun mapCenterOrDefault(hotel: HotelInfo?): LatLng =
    if (hotel?.lat != null && hotel.lng != null) LatLng(hotel.lat, hotel.lng) else LatLng(48.8566, 2.3522)

@Composable
private fun HotelCard(hotel: HotelInfo?, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.day_detail_hotel), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        hotel?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.day_detail_no_hotel),
                        style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary
                    )
                    if (!hotel?.checkIn.isNullOrBlank() || !hotel?.checkOut.isNullOrBlank()) {
                        Text(
                            stringResource(R.string.day_detail_checkin_checkout, hotel?.checkIn.orEmpty().ifBlank { "--" }, hotel?.checkOut.orEmpty().ifBlank { "--" }),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.day_detail_edit_hotel_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun AddFlightButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.FlightTakeoff, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(stringResource(R.string.day_detail_add_flight_button))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotelEditDialog(existing: HotelInfo?, onDismiss: () -> Unit, onSave: (HotelInfo?) -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var lat by remember { mutableStateOf(existing?.lat) }
    var lng by remember { mutableStateOf(existing?.lng) }
    var placeId by remember { mutableStateOf(existing?.placeId) }
    var checkIn by remember { mutableStateOf(existing?.checkIn.orEmpty()) }
    var checkOut by remember { mutableStateOf(existing?.checkOut.orEmpty()) }
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_detail_hotel_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showSearch = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (name.isBlank()) stringResource(R.string.day_detail_search_hotel) else stringResource(R.string.day_detail_change_hotel))
                }
                OutlinedTextField(value = name, onValueChange = { name = it; placeId = null }, label = { Text(stringResource(R.string.day_detail_hotel_name_label)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(stringResource(R.string.day_detail_address_label)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showCheckInPicker = true }, modifier = Modifier.weight(1f)) { Text(if (checkIn.isBlank()) stringResource(R.string.day_detail_checkin_time_label) else stringResource(R.string.day_detail_checkin_prefix, checkIn)) }
                    OutlinedButton(onClick = { showCheckOutPicker = true }, modifier = Modifier.weight(1f)) { Text(if (checkOut.isBlank()) stringResource(R.string.day_detail_checkout_time_label) else stringResource(R.string.day_detail_checkout_prefix, checkOut)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    if (name.isBlank()) null
                    else (existing ?: HotelInfo()).copy(name = name, address = address, checkIn = checkIn, checkOut = checkOut, lat = lat, lng = lng, placeId = placeId)
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )

    if (showCheckInPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_checkin_time_label), checkIn.ifBlank { "15:00" }, { showCheckInPicker = false }, { checkIn = it; showCheckInPicker = false })
    if (showCheckOutPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_checkout_time_label), checkOut.ifBlank { "11:00" }, { showCheckOutPicker = false }, { checkOut = it; showCheckOutPicker = false })
    PlaceSearchMapDialog(
        visible = showSearch, typesFilter = listOf("lodging"), onDismiss = { showSearch = false },
        onPlacePicked = { picked: PickedPlace -> name = picked.name; address = picked.address; lat = picked.lat; lng = picked.lng; placeId = picked.placeId; showSearch = false }
    )
}

private fun queryFileName(resolver: android.content.ContentResolver, uri: Uri): String? {
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
    }
    return null
}

@Composable
private fun FlightCard(flight: FlightInfo?, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.day_detail_flight_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${flight?.airline.orEmpty()} ${flight?.flightNumber.orEmpty()}".trim(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    val route = listOfNotNull(flight?.departureAirportCode?.takeIf { it.isNotBlank() }, flight?.arrivalAirportCode?.takeIf { it.isNotBlank() }).joinToString(" → ")
                    val times = listOfNotNull(flight?.departureTime?.takeIf { it.isNotBlank() }, flight?.arrivalTime?.takeIf { it.isNotBlank() }).joinToString(" – ")
                    if (route.isNotBlank() || times.isNotBlank()) {
                        Text(listOf(route, times).filter { it.isNotBlank() }.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.day_detail_edit_flight_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlightEditDialog(existing: FlightInfo?, date: String, onDismiss: () -> Unit, onSave: (FlightInfo?) -> Unit) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val defaultLookupError = stringResource(R.string.day_detail_lookup_failed_default)

    var airline by remember { mutableStateOf(existing?.airline.orEmpty()) }
    var flightNumber by remember { mutableStateOf(existing?.flightNumber.orEmpty()) }
    var departureCode by remember { mutableStateOf(existing?.departureAirportCode.orEmpty()) }
    var arrivalCode by remember { mutableStateOf(existing?.arrivalAirportCode.orEmpty()) }
    var departureLat by remember { mutableStateOf(existing?.departureAirportLat) }
    var departureLng by remember { mutableStateOf(existing?.departureAirportLng) }
    var arrivalLat by remember { mutableStateOf(existing?.arrivalAirportLat) }
    var arrivalLng by remember { mutableStateOf(existing?.arrivalAirportLng) }
    var departureTime by remember { mutableStateOf(existing?.departureTime.orEmpty()) }
    var arrivalTime by remember { mutableStateOf(existing?.arrivalTime.orEmpty()) }
    var showDeparturePicker by remember { mutableStateOf(false) }
    var showArrivalPicker by remember { mutableStateOf(false) }
    var showDepartureSearch by remember { mutableStateOf(false) }
    var showArrivalSearch by remember { mutableStateOf(false) }

    var flightNumberQuery by remember { mutableStateOf(existing?.flightNumber.orEmpty().replace(" ", "")) }
    var isLookingUp by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<String?>(null) }

    fun lookupFlight() {
        val query = flightNumberQuery.trim()
        if (query.isBlank() || isLookingUp) return
        isLookingUp = true
        lookupError = null
        scope.launch {
            container.flightLookupRepository.lookup(query, date)
                .onSuccess { result ->
                    airline = result.airline.ifBlank { airline }
                    flightNumber = result.flightNumber.ifBlank { flightNumber }
                    if (result.departureAirportCode.isNotBlank()) departureCode = result.departureAirportCode
                    if (result.arrivalAirportCode.isNotBlank()) arrivalCode = result.arrivalAirportCode
                    result.departureAirportLat?.let { departureLat = it }
                    result.departureAirportLng?.let { departureLng = it }
                    result.arrivalAirportLat?.let { arrivalLat = it }
                    result.arrivalAirportLng?.let { arrivalLng = it }
                    if (result.departureTime.isNotBlank()) departureTime = result.departureTime
                    if (result.arrivalTime.isNotBlank()) arrivalTime = result.arrivalTime
                }
                .onFailure { e -> lookupError = e.message ?: defaultLookupError }
            isLookingUp = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_detail_flight_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = flightNumberQuery,
                        onValueChange = { flightNumberQuery = it.uppercase() },
                        label = { Text(stringResource(R.string.day_detail_flight_number_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(onClick = { lookupFlight() }, enabled = !isLookingUp && flightNumberQuery.isNotBlank()) {
                        if (isLookingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.day_detail_look_up))
                        }
                    }
                }
                if (lookupError != null) {
                    Text(lookupError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = airline, onValueChange = { airline = it }, label = { Text(stringResource(R.string.day_detail_airline_label)) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = flightNumber, onValueChange = { flightNumber = it }, label = { Text(stringResource(R.string.day_detail_flight_number_short_label)) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = departureCode, onValueChange = { departureCode = it.uppercase() }, label = { Text(stringResource(R.string.day_detail_from_code_label)) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = arrivalCode, onValueChange = { arrivalCode = it.uppercase() }, label = { Text(stringResource(R.string.day_detail_to_code_label)) }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDepartureSearch = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(if (departureLat == null) stringResource(R.string.day_detail_find_departure_airport) else stringResource(R.string.day_detail_departure_confirmed), maxLines = 1)
                    }
                    OutlinedButton(onClick = { showArrivalSearch = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(if (arrivalLat == null) stringResource(R.string.day_detail_find_arrival_airport) else stringResource(R.string.day_detail_arrival_confirmed), maxLines = 1)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDeparturePicker = true }, modifier = Modifier.weight(1f)) { Text(if (departureTime.isBlank()) stringResource(R.string.day_detail_departs_label) else stringResource(R.string.day_detail_departs_prefix, departureTime)) }
                    OutlinedButton(onClick = { showArrivalPicker = true }, modifier = Modifier.weight(1f)) { Text(if (arrivalTime.isBlank()) stringResource(R.string.day_detail_arrives_label) else stringResource(R.string.day_detail_arrives_prefix, arrivalTime)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    if (airline.isBlank() && flightNumber.isBlank()) null
                    else (existing ?: FlightInfo()).copy(
                        airline = airline, flightNumber = flightNumber,
                        departureAirportCode = departureCode, arrivalAirportCode = arrivalCode,
                        departureAirportLat = departureLat, departureAirportLng = departureLng,
                        arrivalAirportLat = arrivalLat, arrivalAirportLng = arrivalLng,
                        departureTime = departureTime, arrivalTime = arrivalTime
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null); onDismiss() }) { Text(stringResource(R.string.day_detail_remove_flight)) }
        }
    )

    if (showDeparturePicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_departure_time_title), departureTime.ifBlank { "09:00" }, { showDeparturePicker = false }, { departureTime = it; showDeparturePicker = false })
    if (showArrivalPicker) SimpleTimePickerDialog(stringResource(R.string.day_detail_arrival_time_title), arrivalTime.ifBlank { "11:00" }, { showArrivalPicker = false }, { arrivalTime = it; showArrivalPicker = false })
    PlaceSearchMapDialog(visible = showDepartureSearch, typesFilter = listOf("airport"), onDismiss = { showDepartureSearch = false },
        onPlacePicked = { picked -> departureLat = picked.lat; departureLng = picked.lng; showDepartureSearch = false })
    PlaceSearchMapDialog(visible = showArrivalSearch, typesFilter = listOf("airport"), onDismiss = { showArrivalSearch = false },
        onPlacePicked = { picked -> arrivalLat = picked.lat; arrivalLng = picked.lng; showArrivalSearch = false })
}

@Composable
private fun DayNoteCard(note: String, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.StickyNote2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    note.takeIf { it.isNotBlank() } ?: stringResource(R.string.day_detail_day_note_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (note.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canEdit) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.day_detail_edit_day_note_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayNoteEditDialog(existing: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var note by remember { mutableStateOf(existing) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.day_detail_note_dialog_title)) },
        text = { OutlinedTextField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text(stringResource(R.string.day_detail_note_label)) }) },
        confirmButton = { TextButton(onClick = { onSave(note) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}