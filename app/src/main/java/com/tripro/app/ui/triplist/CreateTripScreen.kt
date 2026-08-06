package com.tripro.app.ui.triplist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import coil.compose.AsyncImage
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.ui.components.TriProTextField
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.ui.theme.PillShape
import com.tripro.app.ui.theme.TriProTypography
import androidx.compose.material3.ButtonDefaults
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.DisplayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripRoute(
    ownerId: String,
    ownerName: String,
    onTripCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: CreateTripViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CreateTripViewModel(container.tripRepository, container.cloudinaryRepository, ownerId, ownerName) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = LocalContext.current.contentResolver

    LaunchedEffect(uiState.createdTripId) {
        uiState.createdTripId?.let { onTripCreated(it) }
    }

    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var pickingDates by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) coverImageUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_trip_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(TriProSpacing.marginMobile)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            TriProTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.create_trip_name_label),
                modifier = Modifier.fillMaxWidth()
            )
            TriProTextField(
                value = destination,
                onValueChange = { destination = it },
                label = stringResource(R.string.create_trip_destination_label),
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.trip_edit_cover_photo_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, TriProColors.CardBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (coverImageUri != null) {
                    AsyncImage(
                        model = coverImageUri,
                        contentDescription = stringResource(R.string.create_trip_selected_photo_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                    )
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.trip_edit_tap_to_change), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            stringResource(R.string.trip_edit_add_photo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)) {
                OutlinedButton(
                    onClick = { pickingDates = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TriProColors.OutlineVariant.copy(alpha = 0.3f))
                ) {
                    val startLabel = startDate?.format(DateTimeFormatter.ofPattern("MMM d")) ?: stringResource(R.string.trip_edit_pick_dates)
                    val endLabel = endDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: stringResource(R.string.trip_edit_end_date)
                    Text(
                        if (startDate != null && endDate != null) "$startLabel – $endLabel" else stringResource(R.string.trip_edit_pick_dates),
                        color = TriProColors.OnSurface
                    )
                }
            }

            if (uiState.validationError) {
                Text(stringResource(R.string.create_trip_validation_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val s = startDate
                    val e = endDate
                    if (s != null && e != null) {
                        viewModel.createTrip(contentResolver, name, destination, coverImageUri, s, e)
                    }
                },
                enabled = !uiState.isSaving && name.isNotBlank() && startDate != null && endDate != null,
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TriProColors.Primary,
                    contentColor = TriProColors.OnPrimary
                )
            ) {
                Text(
                    if (uiState.isSaving) stringResource(R.string.create_trip_creating) else stringResource(R.string.create_trip_create_button),
                    style = TriProTypography.labelMedium
                )
            }
        }
    }

    if (pickingDates) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate?.toEpochMilli(),
            initialSelectedEndDateMillis = endDate?.toEpochMilli(),
            initialDisplayMode = DisplayMode.Picker
        )
        val pickerColors = DatePickerDefaults.colors(
            containerColor = Color.Transparent,
            selectedDayContainerColor = TriProColors.Primary,
            selectedDayContentColor = TriProColors.OnPrimary,
            dayInSelectionRangeContainerColor = TriProColors.SecondaryContainer,
            dayInSelectionRangeContentColor = TriProColors.OnSecondaryContainer,
            todayContentColor = TriProColors.Primary,
            todayDateBorderColor = TriProColors.Primary,
            dividerColor = Color.Transparent
        )

        com.tripro.app.ui.components.TriProDialog(
            onDismissRequest = { pickingDates = false },
            padding = PaddingValues(horizontal = 8.dp, vertical = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.trip_edit_pick_dates),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Box(modifier = Modifier.height(450.dp)) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.fillMaxSize(),
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        colors = pickerColors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { pickingDates = false }) {
                        Text(stringResource(R.string.action_cancel), color = TriProColors.OnSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            startDate = dateRangePickerState.selectedStartDateMillis?.toLocalDate()
                            endDate = dateRangePickerState.selectedEndDateMillis?.toLocalDate()
                            pickingDates = false
                        },
                        enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TriProColors.Primary,
                            contentColor = TriProColors.OnPrimary
                        )
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun LocalDate.toEpochMilli(): Long =
    this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
