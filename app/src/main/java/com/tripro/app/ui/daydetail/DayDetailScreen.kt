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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hotel
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.google.android.gms.maps.model.LatLng
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.FlightInfo
import com.tripro.app.data.model.HotelInfo
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.WeatherStatus
import com.tripro.app.ui.components.AttachmentViewerDialog
import com.tripro.app.ui.components.DayMapPreview
import com.tripro.app.ui.components.ItineraryItemRow
import com.tripro.app.ui.components.MapPin
import com.tripro.app.ui.components.WeatherCard
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailRoute(
    tripId: String,
    date: String,
    currentUid: String,
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
                    tripId, date, currentUid
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = LocalContext.current.contentResolver

    var showAddItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ItineraryItem?>(null) }
    var showHotelDialog by remember { mutableStateOf(false) }
    var showFlightDialog by remember { mutableStateOf(false) }
    var showDayNoteDialog by remember { mutableStateOf(false) }
    var pendingAttachmentItemId by remember { mutableStateOf<String?>(null) }
    var viewingAttachment by remember { mutableStateOf<Pair<String, Attachment>?>(null) }

    // WRITE_EXTERNAL_STORAGE is only needed on API 26-28 (see manifest maxSdkVersion) —
    // on API 29+ DownloadManager writes to the public Downloads dir without it.
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op: if denied, the download silently won't start on old OS versions */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // API 28 and below
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val itemId = pendingAttachmentItemId
        if (uri != null && itemId != null) {
            val name = queryFileName(contentResolver, uri) ?: "file"
            viewModel.uploadAttachment(contentResolver, itemId, uri, name)
        }
        pendingAttachmentItemId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Day ${uiState.day?.dayIndex ?: ""}", style = MaterialTheme.typography.bodySmall)
                        Text(DateUtils.formatFullDayLabel(date), style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canEdit) {
                FloatingActionButton(
                    onClick = { editingItem = null; showAddItemSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add to itinerary")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val day = uiState.day
        val pins = buildList {
            day?.hotel?.let { h -> if (h.lat != null && h.lng != null) add(MapPin("Hotel: ${h.name}", h.address, h.lat, h.lng)) }
            uiState.items.forEach { i -> if (i.lat != null && i.lng != null) add(MapPin(i.title, i.locationName, i.lat, i.lng)) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TriProSpacing.marginMobile,
                end = TriProSpacing.marginMobile,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            item {
                HotelCard(hotel = day?.hotel, canEdit = uiState.canEdit, onEdit = { showHotelDialog = true })
            }

            item {
                FlightCard(flight = day?.flight, canEdit = uiState.canEdit, onEdit = { showFlightDialog = true })
            }

            item {
                WeatherCard(
                    weather = uiState.weather,
                    isLoading = uiState.weatherLoading,
                    forecastAvailableFromLabel = if (uiState.weather?.status == WeatherStatus.NOT_YET_AVAILABLE) viewModel.forecastAvailableFromLabel() else null
                )
            }

            if (pins.isNotEmpty()) {
                item { DayMapPreview(pins = pins) }
            }

            item {
                DayNoteCard(note = day?.dayNote.orEmpty(), canEdit = uiState.canEdit, onEdit = { showDayNoteDialog = true })
            }

            item {
                Text(
                    "Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.items.isEmpty()) {
                item {
                    Text(
                        "Nothing planned yet for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.items, key = { it.id }) { item ->
                ItineraryItemRow(
                    item = item,
                    canEdit = uiState.canEdit,
                    onEdit = { editingItem = item; showAddItemSheet = true },
                    onDelete = { viewModel.deleteItem(item.id) },
                    onAddAttachment = {
                        pendingAttachmentItemId = item.id
                        filePicker.launch(arrayOf("*/*"))
                    },
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
        HotelEditDialog(
            existing = uiState.day?.hotel,
            onDismiss = { showHotelDialog = false },
            onSave = { hotel -> viewModel.updateHotel(hotel); showHotelDialog = false }
        )
    }

    if (showFlightDialog) {
        FlightEditDialog(
            existing = uiState.day?.flight,
            onDismiss = { showFlightDialog = false },
            onSave = { flight -> viewModel.updateFlight(flight); showFlightDialog = false }
        )
    }

    if (showDayNoteDialog) {
        DayNoteEditDialog(
            existing = uiState.day?.dayNote.orEmpty(),
            onDismiss = { showDayNoteDialog = false },
            onSave = { note -> viewModel.updateDayNote(note); showDayNoteDialog = false }
        )
    }

    viewingAttachment?.let { (itemId, attachment) ->
        AttachmentViewerDialog(
            attachment = attachment,
            onDismiss = { viewingAttachment = null },
            onRemove = if (uiState.canEdit) ({ viewModel.removeAttachment(itemId, attachment) }) else null
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("BASE CAMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        hotel?.name?.takeIf { it.isNotBlank() } ?: "No hotel set for this day",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit hotel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotelEditDialog(existing: HotelInfo?, onDismiss: () -> Unit, onSave: (HotelInfo?) -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var checkIn by remember { mutableStateOf(existing?.checkIn.orEmpty()) }
    var checkOut by remember { mutableStateOf(existing?.checkOut.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hotel for this day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Hotel name") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = checkIn, onValueChange = { checkIn = it }, label = { Text("Check-in") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = checkOut, onValueChange = { checkOut = it }, label = { Text("Check-out") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    if (name.isBlank()) null
                    else (existing ?: HotelInfo()).copy(name = name, address = address, checkIn = checkIn, checkOut = checkOut)
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("FLIGHT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (flight != null && (flight.airline.isNotBlank() || flight.flightNumber.isNotBlank())) {
                        Text(
                            "${flight.airline} ${flight.flightNumber}".trim(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val route = listOfNotNull(
                            flight.departureAirportCode.takeIf { it.isNotBlank() },
                            flight.arrivalAirportCode.takeIf { it.isNotBlank() }
                        ).joinToString(" → ")
                        val times = listOfNotNull(
                            flight.departureTime.takeIf { it.isNotBlank() },
                            flight.arrivalTime.takeIf { it.isNotBlank() }
                        ).joinToString(" – ")
                        if (route.isNotBlank() || times.isNotBlank()) {
                            Text(
                                listOf(route, times).filter { it.isNotBlank() }.joinToString("  ·  "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("No flight for this day", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit flight", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlightEditDialog(existing: FlightInfo?, onDismiss: () -> Unit, onSave: (FlightInfo?) -> Unit) {
    var airline by remember { mutableStateOf(existing?.airline.orEmpty()) }
    var flightNumber by remember { mutableStateOf(existing?.flightNumber.orEmpty()) }
    var departureCode by remember { mutableStateOf(existing?.departureAirportCode.orEmpty()) }
    var arrivalCode by remember { mutableStateOf(existing?.arrivalAirportCode.orEmpty()) }
    var departureTime by remember { mutableStateOf(existing?.departureTime.orEmpty()) }
    var arrivalTime by remember { mutableStateOf(existing?.arrivalTime.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flight for this day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = airline, onValueChange = { airline = it }, label = { Text("Airline") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = flightNumber, onValueChange = { flightNumber = it }, label = { Text("Flight #") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = departureCode, onValueChange = { departureCode = it }, label = { Text("From (airport code)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = arrivalCode, onValueChange = { arrivalCode = it }, label = { Text("To (airport code)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = departureTime, onValueChange = { departureTime = it }, label = { Text("Departs (HH:mm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = arrivalTime, onValueChange = { arrivalTime = it }, label = { Text("Arrives (HH:mm)") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    if (airline.isBlank() && flightNumber.isBlank()) null
                    else (existing ?: FlightInfo()).copy(
                        airline = airline,
                        flightNumber = flightNumber,
                        departureAirportCode = departureCode,
                        arrivalAirportCode = arrivalCode,
                        departureTime = departureTime,
                        arrivalTime = arrivalTime
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DayNoteCard(note: String, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.StickyNote2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(
                    note.takeIf { it.isNotBlank() } ?: "Add a note for this whole day (e.g. \"Pack light layers, cobblestones today\")",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (note.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit day note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
        title = { Text("Note for this day") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Note") }
            )
        },
        confirmButton = { TextButton(onClick = { onSave(note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
