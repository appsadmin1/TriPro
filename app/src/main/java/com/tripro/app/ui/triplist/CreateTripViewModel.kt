package com.tripro.app.ui.triplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CreateTripUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    val createdTripId: String? = null
)

class CreateTripViewModel(
    private val tripRepository: TripRepository,
    private val ownerId: String,
    private val ownerName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

    fun createTrip(name: String, destination: String, coverImageUrl: String, startDate: LocalDate, endDate: LocalDate) {
        if (name.isBlank() || endDate.isBefore(startDate)) {
            _uiState.value = _uiState.value.copy(error = "Please check the trip name and dates.")
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val id = tripRepository.createTrip(
                    name = name,
                    destination = destination,
                    coverImageUrl = coverImageUrl,
                    startDate = startDate,
                    endDate = endDate,
                    ownerId = ownerId,
                    ownerName = ownerName
                )
                _uiState.value = _uiState.value.copy(isSaving = false, createdTripId = id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Couldn't create the trip.")
            }
        }
    }
}
