package com.tripro.app.ui.tripoverview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class DocEntry(val date: String, val itemId: String, val itemTitle: String, val attachment: Attachment)

data class TripDocsUiState(
    val isLoading: Boolean = true,
    val tripName: String = "",
    val docsByDate: List<Pair<String, List<DocEntry>>> = emptyList(),
    val expandedDates: Set<String> = emptySet()
)

class TripDocsViewModel(private val tripRepository: TripRepository, private val tripId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(TripDocsUiState())
    val uiState: StateFlow<TripDocsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tripRepository.observeTrip(tripId).collect { trip -> _uiState.value = _uiState.value.copy(tripName = trip?.name.orEmpty()) }
        }
        viewModelScope.launch {
            tripRepository.observeDays(tripId)
                .flatMapLatest { days -> tripRepository.observeAllItemsForTrip(tripId, days.map { it.date }) }
                .catch { e -> Log.e("TripDocsViewModel", "Error loading docs: ${e.message}", e); _uiState.value = _uiState.value.copy(isLoading = false) }
                .collect { itemsByDate ->
                    val grouped = itemsByDate.entries.sortedBy { it.key }.mapNotNull { (date, items) ->
                        val entries = items.flatMap { item -> item.attachments.map { att -> DocEntry(date, item.id, item.title, att) } }
                        if (entries.isEmpty()) null else date to entries
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, docsByDate = grouped)
                }
        }
    }

    fun toggleDateExpanded(date: String) {
        val current = _uiState.value.expandedDates
        _uiState.value = _uiState.value.copy(
            expandedDates = if (current.contains(date)) current - date else current + date
        )
    }

    fun expandAll() {
        _uiState.value = _uiState.value.copy(expandedDates = _uiState.value.docsByDate.map { it.first }.toSet())
    }

    fun collapseAll() {
        _uiState.value = _uiState.value.copy(expandedDates = emptySet())
    }

    fun renameAttachment(date: String, itemId: String, attachment: Attachment, newName: String) = viewModelScope.launch {
        runCatching { tripRepository.renameAttachment(tripId, date, itemId, attachment.id, newName) }
    }
}