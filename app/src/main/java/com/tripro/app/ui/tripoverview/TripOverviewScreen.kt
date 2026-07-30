package com.tripro.app.ui.tripoverview

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import coil.compose.AsyncImage
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.TripDay
import com.tripro.app.ui.components.AvatarStack
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.DateUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripOverviewRoute(
    tripId: String,
    currentUid: String,
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
    onOpenCollaborators: () -> Unit,
    onOpenDocuments: (String) -> Unit,
    onTripDeleted: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: TripOverviewViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TripOverviewViewModel(container.tripRepository, container.userRepository, container.cloudinaryRepository, tripId, currentUid) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = LocalContext.current.contentResolver
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.updateCoverImage(contentResolver, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.trip?.name ?: "", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary) }
                },
                actions = {
                    IconButton(onClick = onOpenCollaborators) {
                        Icon(Icons.Filled.Group, contentDescription = "Collaborators", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    // Item 7: only the owner sees the delete option.
                    if (uiState.myRole == Role.OWNER) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete trip", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        if (uiState.isLoading || uiState.trip == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val trip = uiState.trip!!

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box {
                    AsyncImage(
                        model = trip.coverImageUrl, contentDescription = trip.destination, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().height(240.dp)
                            .background(Brush.verticalGradient(listOf(HorizonEthosColors.Primary.copy(alpha = 0.75f), Color.Transparent)))
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text(trip.destination, style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp), color = HorizonEthosColors.OnPrimary)
                        Text(DateUtils.formatRange(trip.startDate, trip.endDate), style = MaterialTheme.typography.bodyLarge, color = HorizonEthosColors.InverseOnSurface)
                    }
                    // Item 5: tap the camera chip to swap the cover photo any time.
                    if (uiState.myRole == Role.OWNER || uiState.myRole == Role.EDITOR) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(HorizonEthosColors.Surface.copy(alpha = 0.85f))
                                .size(40.dp)
                                .clickable {
                                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isUpdatingCover) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Change cover photo", tint = HorizonEthosColors.Primary)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile, vertical = TriProSpacing.stackMd),
                    horizontalArrangement = Arrangement.spacedBy(TriProSpacing.base)
                ) {
                    val days = runCatching { ChronoUnit.DAYS.between(DateUtils.parse(trip.startDate), DateUtils.parse(trip.endDate)) + 1 }.getOrDefault(0)
                    StatChip(icon = Icons.Filled.CalendarMonth, value = "$days Days", label = "DURATION", modifier = Modifier.weight(1f))
                    StatChip(icon = Icons.Filled.Group, value = "${trip.memberIds.size}", label = "TRAVELERS", modifier = Modifier.weight(1f))
                    StatChip(icon = Icons.Filled.HourglassTop, value = DateUtils.countdownLabel(trip.startDate, trip.endDate), label = "STATUS", modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile)
                        .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Travelers", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(6.dp))
                            AvatarStack(photoUrls = uiState.collaboratorAvatars, avatarSize = 36)
                        }
                        if (uiState.myRole == Role.OWNER) {
                            IconButton(onClick = onOpenCollaborators) {
                                Icon(Icons.Filled.Group, contentDescription = "Manage collaborators", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    // Item 1c: the mockup's "Add Activity" / "View Docs" row, previously missing here.
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(
                            onClick = { viewModel.defaultDayForNewActivity()?.let(onOpenDay) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Add Activity")
                        }
                        androidx.compose.material3.OutlinedButton(onClick = { onOpenDocuments(tripId) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("View Docs")
                        }
                    }
                }
                Spacer(Modifier.height(TriProSpacing.stackLg))
            }

            item {
                Text(
                    "Itinerary", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = TriProSpacing.marginMobile, vertical = 8.dp)
                )
            }

            items(uiState.days, key = { it.date }) { day -> DayRow(day = day, onClick = { onOpenDay(day.date) }) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this trip?") },
            text = { Text("This removes \"${uiState.trip?.name}\" for every collaborator. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; viewModel.deleteTrip(onDeleted = onTripDeleted) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun DayRow(day: TripDay, onClick: () -> Unit) {
    val isToday = runCatching { DateUtils.parse(day.date) == LocalDate.now() }.getOrDefault(false)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)).let { if (isToday) it.background(MaterialTheme.colorScheme.surfaceVariant) else it }
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(DateUtils.formatWeekdayShort(day.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(DateUtils.formatDayNumber(day.date), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Day ${day.dayIndex}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (day.hotel != null) Text(day.hotel.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}