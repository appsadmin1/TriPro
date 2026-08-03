package com.tripro.app.ui.tripoverview

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripro.app.R
import com.tripro.app.data.model.Trip
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Edit sheet for an existing trip's name/destination/cover photo/dates — opened from the
 * pencil icon in TripOverviewScreen's TopAppBar (owner-only). Mirrors CreateTripScreen's
 * fields so editing feels the same as creating a trip, plus a destructive delete action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditSheet(
    trip: Trip?,
    onDismiss: () -> Unit,
    onSave: (name: String, destination: String, coverUri: Uri?, start: LocalDate, end: LocalDate) -> Unit,
    onDeleteTrip: () -> Unit
) {
    if (trip == null) return

    var name by remember { mutableStateOf(trip.name) }
    var destination by remember { mutableStateOf(trip.destination) }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var startDate by remember { mutableStateOf(runCatching { LocalDate.parse(trip.startDate) }.getOrNull()) }
    var endDate by remember { mutableStateOf(runCatching { LocalDate.parse(trip.endDate) }.getOrNull()) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) coverImageUri = uri }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(TriProSpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            Text(stringResource(R.string.trip_edit_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.trip_edit_name_label)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text(stringResource(R.string.trip_edit_destination_label)) }, modifier = Modifier.fillMaxWidth())

            Text(stringResource(R.string.trip_edit_cover_photo_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, HorizonEthosColors.CardBorder, RoundedCornerShape(16.dp))
                    .clickable { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                val previewModel = coverImageUri ?: trip.coverImageUrl.takeIf { it.isNotBlank() }
                if (previewModel != null) {
                    AsyncImage(
                        model = previewModel, contentDescription = stringResource(R.string.trip_edit_cover_photo_label), contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))
                    )
                    Box(
                        modifier = Modifier.padding(8.dp).align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.trip_edit_tap_to_change), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.trip_edit_add_photo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)) {
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                    Text(startDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: stringResource(R.string.trip_edit_start_date))
                }
                OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                    Text(endDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: stringResource(R.string.trip_edit_end_date))
                }
            }

            Button(
                onClick = {
                    val s = startDate; val e = endDate
                    if (s != null && e != null) onSave(name, destination, coverImageUri, s, e)
                },
                enabled = name.isNotBlank() && startDate != null && endDate != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.trip_edit_save_changes)) }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }

            Button(
                onClick = { confirmingDelete = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth().padding(top = TriProSpacing.stackLg)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.trip_edit_delete_trip))
            }
        }
    }

    if (pickingStart) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { startDate = it.toLocalDate() }; pickingStart = false }) { Text(stringResource(R.string.action_ok)) } },
            dismissButton = { TextButton(onClick = { pickingStart = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }
    if (pickingEnd) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { endDate = it.toLocalDate() }; pickingEnd = false }) { Text(stringResource(R.string.action_ok)) } },
            dismissButton = { TextButton(onClick = { pickingEnd = false }) { Text(stringResource(R.string.action_cancel)) } }
        ) { DatePicker(state = state) }
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.trip_edit_delete_confirm_title)) },
            text = { Text(stringResource(R.string.trip_edit_delete_confirm_text, trip.name)) },
            confirmButton = { TextButton(onClick = { confirmingDelete = false; onDeleteTrip() }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()