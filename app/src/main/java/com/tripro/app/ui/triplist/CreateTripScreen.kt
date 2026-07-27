package com.tripro.app.ui.triplist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.tripro.app.TriProApplication
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
            initializer { CreateTripViewModel(container.tripRepository, ownerId, ownerName) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdTripId) {
        uiState.createdTripId?.let { onTripCreated(it) }
    }

    var name by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var coverImageUrl by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

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
            OutlinedTextField(
                value = coverImageUrl,
                onValueChange = { coverImageUrl = it },
                label = { Text("Cover image URL (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

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
                    if (s != null && e != null) viewModel.createTrip(name, destination, coverImageUrl, s, e)
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
