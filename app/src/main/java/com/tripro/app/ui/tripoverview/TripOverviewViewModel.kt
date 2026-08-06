package com.tripro.app.ui.tripoverview

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.ActivityColorPrefs
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.TripDay
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import com.tripro.app.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class TripOverviewUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val days: List<TripDay> = emptyList(),
    val itemsByDate: Map<String, List<ItineraryItem>> = emptyMap(),
    val activityColors: ActivityColorPrefs = ActivityColorPrefs(),
    val myRole: Role = Role.VIEWER,
    val collaboratorAvatars: List<String> = emptyList(),
    val memberProfiles: Map<String, UserProfile> = emptyMap(),
    val totalDocsCount: Int = 0,
    val isDeleted: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class TripOverviewViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    private val tripId: String,
    private val currentUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripOverviewUiState())
    val uiState: StateFlow<TripOverviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // First observe trip and days
            val tripAndDaysFlow = combine(tripRepository.observeTrip(tripId), tripRepository.observeDays(tripId)) { trip, days -> trip to days }
            val dataFlow = combine(tripAndDaysFlow, userRepository.observeActivityColors(currentUid)) { (trip, days), colors ->
                Triple(trip, days, colors)
            }

            dataFlow
                .flatMapLatest { (trip, days, colors) ->
                    val dates = days.map { it.date }
                    tripRepository.observeAllItemsForTrip(tripId, dates).map { itemsByDate ->
                        DataTuple(trip, days, colors, itemsByDate)
                    }
                }
                .catch { e ->
                    // PERMISSION_DENIED is expected during trip deletion — ignore it to avoid crashes
                    if (e.message?.contains("PERMISSION_DENIED") != true) {
                        Log.e("TripOverviewViewModel", "Error observing trip data: ${e.message}", e)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (trip, days, colors, itemsByDate) ->
                    val totalDocs = itemsByDate.values.flatten().sumOf { it.attachments.size }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        trip = trip, 
                        days = days, 
                        itemsByDate = itemsByDate,
                        activityColors = colors,
                        myRole = trip?.roleOf(currentUid) ?: Role.VIEWER,
                        totalDocsCount = totalDocs
                    )
                    
                    if (trip != null) {
                        val profiles = userRepository.getProfiles(trip.memberIds)
                        _uiState.value = _uiState.value.copy(
                            memberProfiles = profiles,
                            collaboratorAvatars = trip.memberIds.mapNotNull { profiles[it]?.photoUrl?.takeIf { url -> url.isNotBlank() } }
                        )
                    }
                }
        }
    }

    private data class DataTuple(val trip: Trip?, val days: List<TripDay>, val colors: ActivityColorPrefs, val itemsByDate: Map<String, List<ItineraryItem>>)

    fun addItem(date: String, item: ItineraryItem) = viewModelScope.launch {
        runCatching {
            tripRepository.addItem(tripId, date, item.copy(createdBy = currentUid))
            pushNotificationRepository.notifyItineraryChange(tripId, date, item.title, action = "added")
        }.onFailure { e -> Log.e("TripOverviewViewModel", "Failed to add item: ${e.message}", e) }
    }

    fun updateTripDetails(
        contentResolver: ContentResolver, name: String, destination: String,
        newCoverImageUri: Uri?, startDate: LocalDate, endDate: LocalDate
    ) = viewModelScope.launch {
        runCatching {
            val coverAttachment = newCoverImageUri?.let { uri ->
                cloudinaryRepository.upload(contentResolver, uri, "cover_${System.currentTimeMillis()}.jpg", currentUid)
            }
            
            // Clean up the OLD cover image if we're uploading a new one
            if (coverAttachment != null) {
                _uiState.value.trip?.let { oldTrip ->
                    if (oldTrip.coverImagePublicId.isNotBlank()) {
                        pushNotificationRepository.deleteCloudinaryAsset(tripId, oldTrip.coverImagePublicId, oldTrip.coverImageResourceType)
                    }
                }
            }

            tripRepository.updateTripDetails(
                tripId, name, destination, 
                coverAttachment?.downloadUrl, coverAttachment?.publicId, coverAttachment?.resourceType,
                startDate, endDate
            )
            pushNotificationRepository.notifyTripUpdate(tripId, what = "Trip details")
        }.onFailure { e -> Log.e("TripOverviewViewModel", "Failed to update trip details: ${e.message}", e) }
    }

    fun deleteTrip() = viewModelScope.launch {
        // Backend now handles Cloudinary cleanup of all assets and recursive Firestore delete in one call
        pushNotificationRepository.deleteTrip(tripId).onSuccess {
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }.onFailure { e ->
            Log.e("TripOverviewViewModel", "Failed to delete trip: ${e.message}", e)
            // Still mark as deleted if we want to leave the screen, or show an error
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }
}
