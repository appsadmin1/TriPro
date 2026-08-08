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
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.res.stringResource
import com.tripro.app.R
import com.tripro.app.util.localizedLabel

import com.tripro.app.ui.components.TriProAlertDialog
import com.tripro.app.ui.components.ColorPickerWheel
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(currentUid: String, onBack: () -> Unit, onOpenDrawer: () -> Unit, onSignOut: () -> Unit) {
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu_cd))
                        }
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd))
                        }
                    }
                }
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

            item { Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                SettingsToggleRow(stringResource(R.string.settings_trip_invites_title), stringResource(R.string.settings_trip_invites_subtitle), uiState.notificationPrefs.tripInvites, viewModel::setTripInvites)
            }
            item {
                SettingsToggleRow(stringResource(R.string.settings_itinerary_changes_title), stringResource(R.string.settings_itinerary_changes_subtitle), uiState.notificationPrefs.itineraryChanges, viewModel::setItineraryChanges)
            }
            item {
                SettingsToggleRow(stringResource(R.string.settings_day_updates_title), stringResource(R.string.settings_day_updates_subtitle), uiState.notificationPrefs.dayInfoChanges, viewModel::setDayInfoChanges)

            }

            item { Text(stringResource(R.string.settings_marker_colors), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) }
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
                    Text(key.localizedLabel(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
                ) { Text(stringResource(R.string.settings_sign_out)) }
            }
        }
    }

    colorPickerFor?.let { key ->
        var selectedHex by remember(key) { mutableStateOf(uiState.activityColors.hex(key)) }

        TriProAlertDialog(
            onDismissRequest = { colorPickerFor = null },
            title = stringResource(R.string.settings_choose_color) + " - " + key.localizedLabel(),
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Selected color preview
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(android.graphics.Color.parseColor(selectedHex)))
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )

                    ColorPickerWheel(
                        initialColor = Color(android.graphics.Color.parseColor(selectedHex)),
                        onColorChanged = { color ->
                            val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                            selectedHex = hex
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MarkerColorPalette.chunked(6).forEach { rowColors ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowColors.forEach { hex ->
                                    val isSelected = hex.equals(selectedHex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedHex = hex }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButtonText = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.setActivityColor(key, selectedHex)
                colorPickerFor = null
            },
            dismissButtonText = stringResource(R.string.action_cancel)
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