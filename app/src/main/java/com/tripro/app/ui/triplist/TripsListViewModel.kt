package com.tripro.app.ui.triplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Trip
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TripsListUiState(
    val isLoading: Boolean = true,
    val upcoming: List<Trip> = emptyList(),
    val past: List<Trip> = emptyList(),
    val memberAvatars: Map<String, List<String>> = emptyMap() // tripId -> photo URLs
)

class TripsListViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val currentUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripsListUiState())
    val uiState: StateFlow<TripsListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.observeUserTrips(currentUid).collect { trips ->
                val today = LocalDate.now()
                val upcoming = trips.filter { runCatching { !LocalDate.parse(it.endDate).isBefore(today) }.getOrDefault(true) }
                    .sortedBy { it.startDate }
                val past = trips.filter { runCatching { LocalDate.parse(it.endDate).isBefore(today) }.getOrDefault(false) }
                    .sortedByDescending { it.startDate }

                _uiState.value = _uiState.value.copy(isLoading = false, upcoming = upcoming, past = past)

                // Best-effort avatar resolution; doesn't block showing the list itself.
                val allMemberIds = trips.flatMap { it.memberIds }.distinct()
                val profiles = userRepository.getProfiles(allMemberIds)
                val avatarsByTrip = trips.associate { trip ->
                    trip.id to trip.memberIds.mapNotNull { profiles[it]?.photoUrl?.takeIf { url -> url.isNotBlank() } }
                }
                _uiState.value = _uiState.value.copy(memberAvatars = avatarsByTrip)
            }
        }
    }
}
