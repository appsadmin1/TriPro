package com.tripro.app.ui.tripoverview

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.TripDay
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
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
    val myRole: Role = Role.VIEWER,
    val collaboratorAvatars: List<String> = emptyList(),
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
            combine(tripRepository.observeTrip(tripId), tripRepository.observeDays(tripId)) { trip, days -> trip to days }
                .catch { e -> Log.e("TripOverviewViewModel", "Error observing trip data: ${e.message}", e); _uiState.value = _uiState.value.copy(isLoading = false) }
                .flatMapLatest { (trip, days) ->
                    val dates = days.map { it.date }
                    tripRepository.observeAllItemsForTrip(tripId, dates).map { itemsByDate ->
                        Triple(trip, days, itemsByDate)
                    }
                }
                .collect { (trip, days, itemsByDate) ->
                    val totalDocs = itemsByDate.values.flatten().sumOf { it.attachments.size }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        trip = trip, 
                        days = days, 
                        myRole = trip?.roleOf(currentUid) ?: Role.VIEWER,
                        totalDocsCount = totalDocs
                    )
                    
                    if (trip != null) {
                        val profiles = userRepository.getProfiles(trip.memberIds)
                        _uiState.value = _uiState.value.copy(
                            collaboratorAvatars = trip.memberIds.mapNotNull { profiles[it]?.photoUrl?.takeIf { url -> url.isNotBlank() } }
                        )
                    }
                }
        }
    }

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
            val coverImageUrl = newCoverImageUri?.let { uri ->
                cloudinaryRepository.upload(contentResolver, uri, "cover_${System.currentTimeMillis()}.jpg", currentUid).downloadUrl
            }
            tripRepository.updateTripDetails(tripId, name, destination, coverImageUrl, startDate, endDate)
            pushNotificationRepository.notifyTripUpdate(tripId, what = "Trip details")
        }.onFailure { e -> Log.e("TripOverviewViewModel", "Failed to update trip details: ${e.message}", e) }
    }

    fun deleteTrip() = viewModelScope.launch {
        runCatching { tripRepository.deleteTrip(tripId) }.onFailure { e -> Log.e("TripOverviewViewModel", "Failed to delete trip: ${e.message}", e) }
        _uiState.value = _uiState.value.copy(isDeleted = true)
    }
}