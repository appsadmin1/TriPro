package com.tripro.app.ui.tripoverview

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.ui.unit.em
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow

import androidx.compose.material.icons.filled.Menu

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tripro.app.data.model.ActivityColorPrefs
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.toMarkerColorKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripOverviewRoute(
    tripId: String,
    currentUid: String,
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
    onOpenCollaborators: () -> Unit,
    onOpenDocs: (String) -> Unit,
    onTripDeleted: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    // ... existing viewModel setup ...
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
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu_cd), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = onBack) {
                            val backIcon = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.ArrowForward else Icons.Filled.ArrowBack
                            Icon(backIcon, contentDescription = stringResource(R.string.common_back_cd), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
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

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                // Hero Section
                Box(modifier = Modifier.fillMaxWidth().height(280.dp).shadow(8.dp)) {
                    AsyncImage(model = trip.coverImageUrl, contentDescription = trip.destination, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Text(trip.destination, style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp), color = Color.White)
                        Text(DateUtils.formatRange(trip.startDate, trip.endDate), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                // Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val days = runCatching { ChronoUnit.DAYS.between(DateUtils.parse(trip.startDate), DateUtils.parse(trip.endDate)) + 1 }.getOrDefault(0)
                    StatChip(icon = Icons.Filled.CalendarMonth, value = pluralStringResource(R.plurals.trip_duration_days, days.toInt(), days.toInt()), label = stringResource(R.string.trip_overview_duration), modifier = Modifier.weight(1f))
                    StatChip(icon = Icons.Filled.Folder, value = pluralStringResource(R.plurals.trip_overview_docs_count, uiState.totalDocsCount, uiState.totalDocsCount), label = stringResource(R.string.trip_overview_saved_docs), modifier = Modifier.weight(1f))
                    StatChip(icon = Icons.Filled.Group, value = trip.memberIds.size.toString(), label = stringResource(R.string.trip_overview_travelers), modifier = Modifier.weight(1f))
                }
            }

            item {
                // Collaborators Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = TriProSpacing.marginMobile).shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.trip_overview_travelers), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            AvatarStack(photoUrls = uiState.collaboratorAvatars, avatarSize = 36)
                        }
                        Row {
                            Button(
                                onClick = {
                                    val today = LocalDate.now().toString()
                                    newItemDate = uiState.days.firstOrNull { it.date >= today }?.date ?: uiState.days.firstOrNull()?.date
                                    showAddItemSheet = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text(stringResource(R.string.trip_overview_add_activity), style = MaterialTheme.typography.labelLarge) }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onOpenDocs(tripId) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) { Text(stringResource(R.string.trip_overview_view_docs), style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text(
                    stringResource(R.string.trip_overview_itinerary),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = TriProSpacing.marginMobile, vertical = 8.dp)
                )
            }

            items(uiState.days, key = { it.date }) { day -> 
                DayRow(
                    day = day, 
                    items = uiState.itemsByDate[day.date] ?: emptyList(),
                    activityColors = uiState.activityColors,
                    onClick = { onOpenDay(day.date) }
                ) 
            }
        }
    }
    // ... dialogs ...

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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(24.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DayRow(day: TripDay, items: List<ItineraryItem>, activityColors: ActivityColorPrefs, onClick: () -> Unit) {
    val isToday = runCatching { DateUtils.parse(day.date) == LocalDate.now() }.getOrDefault(false)
    
    // Outer container matching design shadow and background
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isToday) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
        border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TriProSpacing.marginMobile, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day/Date Column
            Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    DateUtils.formatWeekdayShort(day.date).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal),
                    color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline
                )
                Text(
                    DateUtils.formatDayNumber(day.date),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Vertical Timeline Line with Dot
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(if (isToday) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isToday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
                        .let { if (isToday) it.border(2.dp, Color.White, CircleShape) else it }
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (day.hotel != null) day.hotel.name else stringResource(R.string.day_row_free_day),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    stringResource(R.string.trip_overview_day_label, day.dayIndex),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Design-inspired activity indicator bars (dynamic based on items)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (day.hotel != null) {
                        Box(modifier = Modifier.size(16.dp, 4.dp).clip(CircleShape).background(Color(activityColors.colorInt(MarkerColorKey.HOTEL))))
                    }
                    if (day.flight != null) {
                        Box(modifier = Modifier.size(16.dp, 4.dp).clip(CircleShape).background(Color(activityColors.colorInt(MarkerColorKey.FLIGHT))))
                    }
                    items.distinctBy { it.type }.forEach { item ->
                        Box(modifier = Modifier.size(16.dp, 4.dp).clip(CircleShape).background(Color(activityColors.colorInt(item.type.toMarkerColorKey()))))
                    }
                    if (day.hotel == null && day.flight == null && items.isEmpty()) {
                        Box(modifier = Modifier.size(24.dp, 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    }
                }
            }
        }
    }
}
