package com.tripro.app.ui.sharedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.UserProfile
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SharedTripsUiState(
    val isLoading: Boolean = true,
    val trips: List<Trip> = emptyList(),
    val targetProfile: UserProfile? = null,
    val error: String? = null
)

class SharedTripsViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val currentUid: String,
    private val targetUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedTripsUiState())
    val uiState: StateFlow<SharedTripsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tripsFlow = tripRepository.observeUserTrips(currentUid)
            
            tripsFlow
                .catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
                .collect { allTrips ->
                    val sharedTrips = allTrips.filter { it.memberIds.contains(targetUid) }
                    val targetProfile = userRepository.getProfiles(listOf(targetUid))[targetUid]
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        trips = sharedTrips,
                        targetProfile = targetProfile
                    )
                }
        }
    }
}
