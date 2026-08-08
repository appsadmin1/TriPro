package com.tripro.app.ui.daydetail

import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.maps.model.LatLng
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.WeatherStatus
import com.tripro.app.data.model.toMarkerColorKey
import com.tripro.app.ui.components.AttachmentViewerDialog
import com.tripro.app.ui.components.DayMapPreview
import com.tripro.app.ui.components.ItineraryItemRow
import com.tripro.app.ui.components.MapPin
import com.tripro.app.ui.components.TriProAlertDialog
import com.tripro.app.ui.components.TriProTextField
import com.tripro.app.ui.components.WeatherCard
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.DateUtils
import com.tripro.app.util.localizedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailRoute(
    tripId: String,
    date: String,
    currentUid: String,
    currentUserName: String,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenDocs: () -> Unit
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
                    container.activityRepository,
                    container.userRepository,
                    tripId, date, currentUid, currentUserName
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
    var showDayNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
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
                        Text(stringResource(R.string.day_detail_day_label, uiState.day?.dayIndex?.toString().orEmpty()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DateUtils.formatFullDayLabel(date), style = MaterialTheme.typography.headlineMedium)
                    }
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu_cd), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onBack) {
                            val backIcon = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.ArrowForward else Icons.Filled.ArrowBack
                            Icon(backIcon, contentDescription = stringResource(R.string.common_back_cd), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
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
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(percent = 50)
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
            uiState.items.forEach { i ->
                if (i.lat != null && i.lng != null) {
                    add(MapPin(i.title, i.locationName, i.lat, i.lng, colorInt = activityColors.colorInt(i.type.toMarkerColorKey())))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(
                start = TriProSpacing.marginMobile, end = TriProSpacing.marginMobile,
                top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            item {
                WeatherCard(
                    weather = uiState.weather, isLoading = uiState.weatherLoading,
                    forecastAvailableFromLabel = if (uiState.weather?.status == WeatherStatus.NOT_YET_AVAILABLE) viewModel.forecastAvailableFromLabel() else null
                )
            }

            if (pins.isNotEmpty()) {
                item { DayMapPreview(pins = pins) }
            }

            if (isEditMode || !day?.dayNote.isNullOrBlank()) {
                item { DayNoteCard(note = day?.dayNote.orEmpty(), canEdit = editingAllowed, onEdit = { showDayNoteDialog = true }) }
            }

            item {
                Text(
                    stringResource(R.string.day_detail_schedule),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.groupedItems.isEmpty()) {
                item { Text(stringResource(R.string.day_detail_nothing_planned), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            uiState.groupedItems.forEach { periodGroup ->
                item {
                    Text(
                        periodGroup.period.localizedLabel().uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )
                }
                
                periodGroup.timeGroups.forEach { timeGroup ->
                    if (timeGroup.label != null) {
                        item {
                            Text(
                                timeGroup.label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                    
                    items(timeGroup.items, key = { it.id }) { item ->
                        ItineraryItemRow(
                            item = item,
                            activityColors = uiState.activityColors,
                            canEdit = editingAllowed,
                            onEdit = {
                                editingItem = item; showAddItemSheet = true
                            },
                            onDelete = { showDeleteConfirm = item.id },
                            onAddAttachment = { pendingAttachmentItemId = item.id; filePicker.launch(arrayOf("*/*")) },
                            onAttachmentClick = { attachment -> viewingAttachment = item.id to attachment },
                            onViewAllDocs = onOpenDocs,
                            onMoveUp = { viewModel.moveItem(item.id, -1) },
                            onMoveDown = { viewModel.moveItem(item.id, 1) }
                        )
                    }
                }
            }
        }
    }

    if (showAddItemSheet) {
        AddEditItemSheet(
            existing = editingItem,
            defaultMapCenter = mapCenterOrDefault(uiState.items),
            onDismiss = { showAddItemSheet = false },
            selectedDate = date,
            onSave = { item ->
                if (editingItem == null) viewModel.addItem(item) else viewModel.updateItem(item)
                showAddItemSheet = false
            },
            currentUid = currentUid
        )
    }

    if (showDayNoteDialog) {
        DayNoteEditDialog(existing = uiState.day?.dayNote.orEmpty(), onDismiss = { showDayNoteDialog = false }, onSave = { note -> viewModel.updateDayNote(note); showDayNoteDialog = false })
    }

    showDeleteConfirm?.let { itemId ->
        TriProAlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = stringResource(R.string.itinerary_delete_confirm_title),
            text = stringResource(R.string.itinerary_delete_confirm_text),
            confirmButtonText = stringResource(R.string.action_delete),
            onConfirm = { viewModel.deleteItem(itemId); showDeleteConfirm = null },
            dismissButtonText = stringResource(R.string.action_cancel),
            isDestructive = true
        )
    }

    viewingAttachment?.let { (itemId, attachment) ->
        AttachmentViewerDialog(
            attachment = attachment,
            onDismiss = { viewingAttachment = null },
            onRemove = if (uiState.canEdit) ({ viewModel.removeAttachment(itemId, attachment.id) }) else null,
            onRename = if (uiState.canEdit) ({ newName -> viewModel.renameAttachment(itemId, attachment, newName) }) else null
        )
    }

    pendingUpload?.let { (itemId, uri) ->
        TriProAlertDialog(
            onDismissRequest = { pendingUpload = null },
            title = stringResource(R.string.day_detail_name_file_title),
            content = {
                TriProTextField(
                    value = pendingUploadName,
                    onValueChange = { pendingUploadName = it },
                    label = stringResource(R.string.day_detail_name_file_title)
                )
            },
            confirmButtonText = stringResource(R.string.action_upload),
            onConfirm = {
                viewModel.uploadAttachment(contentResolver, itemId, uri, pendingUploadName.ifBlank { "file" })
                pendingUpload = null
            },
            dismissButtonText = stringResource(R.string.action_cancel)
        )
    }
}

private fun mapCenterOrDefault(items: List<ItineraryItem>): LatLng {
    val firstHotel = items.firstOrNull { it.type == ItemType.HOTEL }
    if (firstHotel?.lat != null && firstHotel.lng != null) return LatLng(firstHotel.lat, firstHotel.lng)
    return LatLng(48.8566, 2.3522)
}

private fun queryFileName(resolver: android.content.ContentResolver, uri: Uri): String? {
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
    }
    return null
}

@Composable
private fun DayNoteCard(note: String, canEdit: Boolean, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, TriProColors.CardBorder),
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
    TriProAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.day_detail_note_dialog_title),
        content = {
            TriProTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                singleLine = false,
                label = stringResource(R.string.day_detail_note_label)
            )
        },
        confirmButtonText = stringResource(R.string.action_save),
        onConfirm = { onSave(note) },
        dismissButtonText = stringResource(R.string.action_cancel)
    )
}
