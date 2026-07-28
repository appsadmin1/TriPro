package com.tripro.app.ui.tripoverview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.TripDay
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TripOverviewUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val days: List<TripDay> = emptyList(),
    val myRole: Role = Role.VIEWER,
    val collaboratorAvatars: List<String> = emptyList()
)

class TripOverviewViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val tripId: String,
    private val currentUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripOverviewUiState())
    val uiState: StateFlow<TripOverviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tripRepository.observeTrip(tripId),
                tripRepository.observeDays(tripId)
            ) { trip, days -> trip to days }
                .catch { e ->
                    Log.e("TripOverviewViewModel", "Error observing trip data: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (trip, days) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    trip = trip,
                    days = days,
                    myRole = trip?.roleOf(currentUid) ?: Role.VIEWER
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
}
