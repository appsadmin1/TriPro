package com.tripro.app.ui.triplist

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CreateTripUiState(
    val isSaving: Boolean = false,
    /** Static, always-localizable validation failure — resolved to text by
     *  CreateTripScreen via stringResource(), since a ViewModel has no Composable
     *  context of its own. */
    val validationError: Boolean = false,
    /** Exception-derived message (Firebase/Cloudinary), left in whatever language the
     *  underlying SDK throws in — a known localization gap, not fixed here. */
    val error: String? = null,
    val createdTripId: String? = null
)

class CreateTripViewModel(
    private val tripRepository: TripRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val ownerId: String,
    private val ownerName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTripUiState())
    val uiState: StateFlow<CreateTripUiState> = _uiState.asStateFlow()

    fun createTrip(
        contentResolver: ContentResolver,
        name: String,
        destination: String,
        coverImageUri: Uri?,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (name.isBlank() || endDate.isBefore(startDate)) {
            _uiState.value = _uiState.value.copy(validationError = true)
            return
        }
        _uiState.value = _uiState.value.copy(isSaving = true, error = null, validationError = false)
        viewModelScope.launch {
            try {
                val coverImageUrl = coverImageUri?.let { uri ->
                    cloudinaryRepository.upload(
                        contentResolver = contentResolver,
                        fileUri = uri,
                        fileName = "cover_${System.currentTimeMillis()}.jpg",
                        uploadedBy = ownerId
                    ).downloadUrl
                }.orEmpty()

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