package com.tripro.app.ui.daydetail

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.ActivityColorPrefs
import com.tripro.app.data.model.ActivityType
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.DailyWeather
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.TripDay
import com.tripro.app.data.model.canEdit
import com.tripro.app.data.repository.ActivityRepository
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import com.tripro.app.data.repository.WeatherRepository
import com.tripro.app.util.PeriodGroup
import com.tripro.app.util.groupByHierarchy
import com.tripro.app.util.ItineraryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class DayDetailUiState(
    val isLoading: Boolean = true,
    val day: TripDay? = null,
    val items: List<ItineraryItem> = emptyList(),
    val weather: DailyWeather? = null,
    val weatherLoading: Boolean = true,
    val canEdit: Boolean = false,
    val activityColors: ActivityColorPrefs = ActivityColorPrefs(),
    val groupedItems: List<PeriodGroup> = emptyList(),
    val uploadingAttachmentForItemId: String? = null,
    val error: String? = null
)

class DayDetailViewModel(
    private val tripRepository: TripRepository,
    private val weatherRepository: WeatherRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val tripId: String,
    private val date: String,
    private val currentUid: String,
    private val currentUserName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()
    private var currentTripName: String = ""
    private var currentMemberIds: List<String> = emptyList()

    init {
        viewModelScope.launch {
            tripRepository.observeTrip(tripId)
                .catch { e ->
                    if (e.message?.contains("PERMISSION_DENIED") != true) {
                        Log.e("DayDetailViewModel", "Error observing trip: ${e.message}", e)
                    }
                }
                .collect { trip -> 
                    currentTripName = trip?.name.orEmpty()
                    currentMemberIds = trip?.memberIds.orEmpty()
                    _uiState.value = _uiState.value.copy(canEdit = trip?.roleOf(currentUid)?.canEdit() ?: false) 
                }
        }

        viewModelScope.launch {
            combine(tripRepository.observeDay(tripId, date), tripRepository.observeItems(tripId, date)) { day, items ->
                day to items
            }
                .catch { e ->
                    if (e.message?.contains("PERMISSION_DENIED") != true) {
                        Log.e("DayDetailViewModel", "Error observing day details: ${e.message}", e)
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (day, items) -> 
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        day = day, 
                        items = items,
                        groupedItems = items.groupByHierarchy()
                    ) 
                }
        }

        viewModelScope.launch {
            userRepository.observeActivityColors(currentUid)
                .catch { /* fall back to defaults silently — cosmetic only */ }
                .collect { colors -> _uiState.value = _uiState.value.copy(activityColors = colors) }
        }

        viewModelScope.launch {
            _uiState.map { state ->
                val firstHotel = state.items.firstOrNull { it.type == ItemType.HOTEL }
                firstHotel?.lat to firstHotel?.lng
            }
                .distinctUntilChanged()
                .collect { (lat, lng) ->
                    _uiState.value = _uiState.value.copy(weatherLoading = true)
                    val weather = weatherRepository.getDailyWeather(lat, lng, date)
                    _uiState.value = _uiState.value.copy(weather = weather, weatherLoading = false)
                }
        }
    }

    fun forecastAvailableFromLabel(): String = weatherRepository.forecastAvailableFrom(date)

    fun addItem(item: ItineraryItem) = launchCatching {
        val nextOrder = (_uiState.value.items.maxOfOrNull { it.order } ?: -1) + 1
        tripRepository.addItem(tripId, date, item.copy(createdBy = currentUid, order = nextOrder))
        pushNotificationRepository.notifyItineraryChange(tripId, date, item.title, action = "added", actorName = currentUserName)
        logActivity(ActivityType.ITEM_ADDED, "${item.title} was added to the itinerary")
    }

    fun updateItem(item: ItineraryItem) = launchCatching {
        tripRepository.updateItem(tripId, date, item, updatedBy = currentUid)
        pushNotificationRepository.notifyItineraryChange(tripId, date, item.title, action = "updated", actorName = currentUserName)
        logActivity(ActivityType.ITEM_UPDATED, "${item.title} was updated")
    }

    fun deleteItem(itemId: String) = launchCatching {
        val title = _uiState.value.items.find { it.id == itemId }?.title ?: "An item"
        // Backend handles Firestore deletion, Cloudinary cleanup, and notification in one go
        pushNotificationRepository.deleteItem(tripId, date, itemId, currentUserName).getOrThrow()
        logActivity(ActivityType.ITEM_REMOVED, "$title was removed from the itinerary")
    }

    fun updateDayNote(note: String) = launchCatching {
        tripRepository.updateDayNote(tripId, date, note, updatedBy = currentUid)
        pushNotificationRepository.notifyDayChange(tripId, date, what = "A note", actorName = currentUserName)
        logActivity(ActivityType.DAY_NOTE_UPDATED, "A note was updated for $date")
    }

    fun moveItem(itemId: String, direction: Int) = launchCatching {
        val currentItems = _uiState.value.items
        val currentIndex = currentItems.indexOfFirst { it.id == itemId }
        if (currentIndex == -1) return@launchCatching

        val targetIndex = currentIndex + direction
        if (targetIndex in currentItems.indices) {
            val item1 = currentItems[currentIndex]
            val item2 = currentItems[targetIndex]

            // Only allow reordering within the same time period
            if (ItineraryUtils.getEffectivePeriod(item1) == ItineraryUtils.getEffectivePeriod(item2)) {
                // Use indices as new orders to ensure they are distinct and correctly swapped
                // We must use a value that will definitely change the sort order.
                // If we use currentIndex and targetIndex, and the repository sorts by order, it will work.
                tripRepository.swapItemOrders(tripId, date, item1.id, targetIndex, item2.id, currentIndex)
            }
        }
    }

    private fun logActivity(type: ActivityType, message: String) {
        if (currentTripName.isBlank() || currentMemberIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                activityRepository.log(tripId, currentTripName, currentMemberIds, type, message, currentUid, currentUserName, date)
            }
        }
    }

    fun uploadAttachment(contentResolver: ContentResolver, itemId: String, uri: Uri, fileName: String) {
        _uiState.value = _uiState.value.copy(uploadingAttachmentForItemId = itemId)
        viewModelScope.launch {
            try {
                val attachment: Attachment = cloudinaryRepository.upload(contentResolver, uri, fileName, currentUid)
                val current = _uiState.value.items.firstOrNull { it.id == itemId }
                if (current != null) {
                    tripRepository.updateItem(tripId, date, current.copy(attachments = current.attachments + attachment), updatedBy = currentUid)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Upload failed")
            } finally {
                _uiState.value = _uiState.value.copy(uploadingAttachmentForItemId = null)
            }
        }
    }

    fun removeAttachment(itemId: String, attachmentId: String) = launchCatching {
        // Backend handles both Firestore update and Cloudinary cleanup for both standard and synthetic items
        pushNotificationRepository.deleteAttachment(tripId, date, itemId, attachmentId).onFailure { throw it }
    }

    /** Renames an attachment's display name in place — a pure Firestore field edit, since
     *  Cloudinary's own asset id never needs to change. */
    fun renameAttachment(itemId: String, attachment: Attachment, newName: String) = launchCatching {
        tripRepository.renameAttachment(tripId, date, itemId, attachment.id, newName)
    }

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: Exception) { _uiState.value = _uiState.value.copy(error = e.message ?: "Something went wrong") }
        }
    }
}
