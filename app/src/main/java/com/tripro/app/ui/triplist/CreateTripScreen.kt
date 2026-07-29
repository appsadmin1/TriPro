package com.tripro.app.ui.triplist

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
import androidx.compose.material3.DatePicker
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import coil.compose.AsyncImage
import com.tripro.app.TriProApplication
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    // Android's built-in Photo Picker — no storage permission needed on any API level,
    // which is why this replaces what used to be a plain "paste a URL" text field.
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) coverImageUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Vacation") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Cancel") }
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip name (e.g. Kyoto Retreat)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Cover photo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, HorizonEthosColors.CardBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (coverImageUri != null) {
                    AsyncImage(
                        model = coverImageUri,
                        contentDescription = "Selected cover photo",
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
                        Text("Tap to change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Add a photo from your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)) {
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.weight(1f)) {
                    Text(startDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Start date")
                }
                OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.weight(1f)) {
                    Text(endDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "End date")
                }
            }

            if (uiState.error != null) {
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(if (uiState.isSaving) "Creating…" else "Create Vacation")
            }
        }
    }

    if (pickingStart) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDate = it.toLocalDate() }
                    pickingStart = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingStart = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (pickingEnd) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDate = it.toLocalDate() }
                    pickingEnd = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingEnd = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
