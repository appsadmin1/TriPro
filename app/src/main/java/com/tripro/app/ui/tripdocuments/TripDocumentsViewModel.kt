package com.tripro.app.ui.tripdocuments

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.repository.TripAttachmentEntry
import com.tripro.app.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Only created lazily when someone opens "View Docs" for a specific trip (not at
 *  nav-graph level), so this one couldn't cause the boot-time crash — but it hits the
 *  same collectionGroup pattern, so it gets the same guard for consistency. */
class TripDocumentsViewModel(
    tripRepository: TripRepository,
    tripId: String
) : ViewModel() {
    private val _entries = MutableStateFlow<List<TripAttachmentEntry>>(emptyList())
    val entries: StateFlow<List<TripAttachmentEntry>> = _entries.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.observeAllAttachments(tripId)
                .catch { e ->
                    Log.e("TripDocumentsViewModel", "Error observing attachments: ${e.message}", e)
                    _isLoading.value = false
                }
                .collect {
                    _entries.value = it
                    _isLoading.value = false
                }
        }
    }
}