package com.tripro.app.ui.tripoverview

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.HotelInfo
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.TripDay
import com.tripro.app.ui.components.AvatarStack
import com.tripro.app.ui.daydetail.AddEditItemSheet
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.DateUtils
import com.tripro.app.util.localizedLabel
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
    onOpenDocs: (String) -> Unit,
    onTripDeleted: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: TripOverviewViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TripOverviewViewModel(
                    container.tripRepository, container.userRepository,
                    container.cloudinaryRepository, container.pushNotificationRepository,
                    tripId, currentUid
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = LocalContext.current.contentResolver

    var showEditSheet by remember { mutableStateOf(false) }
    var showAddItemSheet by remember { mutableStateOf(false) }
    var newItemDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onTripDeleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.trip?.name ?: "", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd), tint = MaterialTheme.colorScheme.onPrimary) }
                },
                actions = {
                    if (uiState.myRole == Role.OWNER) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.trip_overview_edit_trip_cd), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    IconButton(onClick = onOpenCollaborators) {
                        Icon(Icons.Filled.Group, contentDescription = stringResource(R.string.trip_overview_collaborators_cd), tint = MaterialTheme.colorScheme.onPrimary)
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
                    AsyncImage(model = trip.coverImageUrl, contentDescription = trip.destination, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(240.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(Brush.verticalGradient(listOf(HorizonEthosColors.Primary.copy(alpha = 0.75f), Color.Transparent))))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                        Text(trip.destination, style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp), color = HorizonEthosColors.OnPrimary)
                        Text(DateUtils.formatRange(trip.startDate, trip.endDate), style = MaterialTheme.typography.bodyLarge, color = HorizonEthosColors.InverseOnSurface)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile, vertical = TriProSpacing.stackMd),
                    horizontalArrangement = Arrangement.spacedBy(TriProSpacing.base)
                ) {
                    val days = runCatching { ChronoUnit.DAYS.between(DateUtils.parse(trip.startDate), DateUtils.parse(trip.endDate)) + 1 }.getOrDefault(0)
                    StatChip(icon = Icons.Filled.CalendarMonth, value = pluralStringResource(R.plurals.trip_duration_days, days.toInt(), days.toInt()), label = stringResource(R.string.trip_overview_duration_label), modifier = Modifier.weight(1f))
                    StatChip(icon = Icons.Filled.Group, value = "${trip.memberIds.size}", label = stringResource(R.string.trip_overview_travelers_label), modifier = Modifier.weight(1f))
                    StatChip(icon = Icons.Filled.HourglassTop, value = DateUtils.countdown(trip.startDate, trip.endDate).localizedLabel(), label = stringResource(R.string.trip_overview_status_label), modifier = Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.trip_overview_travelers), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        AvatarStack(photoUrls = uiState.collaboratorAvatars, avatarSize = 36)
                    }
                    if (uiState.myRole == Role.OWNER) {
                        IconButton(onClick = onOpenCollaborators) {
                            Icon(Icons.Filled.Group, contentDescription = stringResource(R.string.trip_overview_manage_collaborators_cd), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile, vertical = TriProSpacing.stackMd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val today = runCatching { LocalDate.now().toString() }.getOrNull().orEmpty()
                            newItemDate = uiState.days.firstOrNull { it.date >= today }?.date ?: uiState.days.firstOrNull()?.date
                            showAddItemSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.trip_overview_add_activity)) }
                    OutlinedButton(onClick = { onOpenDocs(tripId) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.trip_overview_view_docs))
                    }
                }
                Spacer(Modifier.height(TriProSpacing.stackLg))
            }

            item {
                Text(stringResource(R.string.trip_overview_itinerary), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = TriProSpacing.marginMobile, vertical = 8.dp))
            }

            items(uiState.days, key = { it.date }) { day -> DayRow(day = day, onClick = { onOpenDay(day.date) }) }
        }
    }

    if (showAddItemSheet) {
        AddEditItemSheet(
            existing = null,
            defaultMapCenter = mapCenterOrDefault(uiState.days.firstOrNull { it.date == newItemDate }?.hotel),
            onDismiss = { showAddItemSheet = false },
            onSave = { item -> newItemDate?.let { date -> viewModel.addItem(date, item) }; showAddItemSheet = false },
            dateOptions = uiState.days.map { it.date to "${DateUtils.formatWeekdayShort(it.date)} ${DateUtils.formatDayNumber(it.date)}" },
            selectedDate = newItemDate,
            onDateSelected = { newItemDate = it }
        )
    }

    if (showEditSheet) {
        TripEditSheet(
            trip = uiState.trip,
            onDismiss = { showEditSheet = false },
            onSave = { name, destination, coverUri, start, end ->
                viewModel.updateTripDetails(contentResolver, name, destination, coverUri, start, end)
                showEditSheet = false
            },
            onDeleteTrip = { viewModel.deleteTrip(); showEditSheet = false }
        )
    }
}

private fun mapCenterOrDefault(hotel: HotelInfo?): LatLng =
    if (hotel?.lat != null && hotel.lng != null) LatLng(hotel.lat, hotel.lng) else LatLng(48.8566, 2.3522)

@Composable
private fun StatChip(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DayRow(day: TripDay, onClick: () -> Unit) {
    val isToday = runCatching { DateUtils.parse(day.date) == LocalDate.now() }.getOrDefault(false)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .let { if (isToday) it.background(MaterialTheme.colorScheme.surfaceVariant) else it }
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(DateUtils.formatWeekdayShort(day.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(DateUtils.formatDayNumber(day.date), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.trip_overview_day_label, day.dayIndex), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (day.hotel != null) Text(day.hotel.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}