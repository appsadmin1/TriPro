package com.tripro.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.MarkerColorPalette
import com.tripro.app.ui.theme.TriProSpacing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(currentUid: String, onBack: () -> Unit, onSignOut: () -> Unit) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.userRepository, container.authRepository, currentUid) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    var colorPickerFor by remember { mutableStateOf<MarkerColorKey?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(TriProSpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackLg)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = uiState.photoUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(uiState.displayName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text(uiState.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Text("Notifications", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                SettingsToggleRow("Trip invites", "When you're added to a trip", uiState.notificationPrefs.tripInvites, viewModel::setTripInvites)
            }
            item {
                SettingsToggleRow("Itinerary changes", "When someone adds, edits, or removes an activity", uiState.notificationPrefs.itineraryChanges, viewModel::setItineraryChanges)
            }
            item {
                SettingsToggleRow("Trip & day updates", "Hotel, flight, day notes, and trip detail changes", uiState.notificationPrefs.dayInfoChanges, viewModel::setDayInfoChanges)
            }

            item { Text("Map marker colors", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }
            items(MarkerColorKey.entries) { key ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { colorPickerFor = key }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(key.displayLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(uiState.activityColors.hex(key))))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
            }

            item {
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) { Text("Sign out") }
            }
        }
    }

    colorPickerFor?.let { key ->
        AlertDialog(
            onDismissRequest = { colorPickerFor = null },
            title = { Text("Choose a color") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MarkerColorPalette.chunked(4).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowColors.forEach { hex ->
                                val isSelected = hex.equals(uiState.activityColors.hex(key), ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.setActivityColor(key, hex); colorPickerFor = null }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { colorPickerFor = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}