package com.tripro.app.ui.tripoverview

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.TripDay
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TripOverviewUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val days: List<TripDay> = emptyList(),
    val myRole: Role = Role.VIEWER,
    val collaboratorAvatars: List<String> = emptyList(),
    val isUpdatingCover: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

class TripOverviewViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val tripId: String,
    private val currentUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripOverviewUiState())
    val uiState: StateFlow<TripOverviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(tripRepository.observeTrip(tripId), tripRepository.observeDays(tripId)) { trip, days -> trip to days }
                .catch { e ->
                    Log.e("TripOverviewViewModel", "Error observing trip data: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (trip, days) ->
                    _uiState.value = _uiState.value.copy(isLoading = false, trip = trip, days = days, myRole = trip?.roleOf(currentUid) ?: Role.VIEWER)
                    if (trip != null) {
                        val profiles = userRepository.getProfiles(trip.memberIds)
                        _uiState.value = _uiState.value.copy(
                            collaboratorAvatars = trip.memberIds.mapNotNull { profiles[it]?.photoUrl?.takeIf { url -> url.isNotBlank() } }
                        )
                    }
                }
        }
    }

    /** Item 5. */
    fun updateCoverImage(contentResolver: ContentResolver, uri: Uri) {
        _uiState.value = _uiState.value.copy(isUpdatingCover = true, error = null)
        viewModelScope.launch {
            try {
                val uploaded = cloudinaryRepository.upload(contentResolver, uri, "cover_${System.currentTimeMillis()}.jpg", currentUid)
                tripRepository.updateCoverImage(tripId, uploaded.downloadUrl)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Couldn't update the cover photo.")
            } finally {
                _uiState.value = _uiState.value.copy(isUpdatingCover = false)
            }
        }
    }

    /** Item 7 — the confirmation prompt itself lives in TripOverviewScreen; this only
     *  runs once that's already been accepted. */
    fun deleteTrip(onDeleted: () -> Unit) {
        if (_uiState.value.myRole != Role.OWNER) return
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)
        viewModelScope.launch {
            try {
                tripRepository.deleteTrip(tripId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isDeleting = false, error = e.message ?: "Couldn't delete the trip.")
            }
        }
    }

    /** "Add Activity" jumps to today's day if it's within the trip, else the first day. */
    fun defaultDayForNewActivity(): String? {
        val days = _uiState.value.days
        if (days.isEmpty()) return null
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return days.firstOrNull { it.date == today }?.date ?: days.first().date
    }
}