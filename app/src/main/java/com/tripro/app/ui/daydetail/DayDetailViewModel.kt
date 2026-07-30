package com.tripro.app.ui.daydetail

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.ActivityType
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.DailyWeather
import com.tripro.app.data.model.FlightInfo
import com.tripro.app.data.model.HotelInfo
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.TripDay
import com.tripro.app.data.model.canEdit
import com.tripro.app.data.repository.ActivityRepository
import com.tripro.app.data.repository.CloudinaryRepository
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.WeatherRepository
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
    val uploadingAttachmentForItemId: String? = null,
    val error: String? = null
)

class DayDetailViewModel(
    private val tripRepository: TripRepository,
    private val weatherRepository: WeatherRepository,
    private val cloudinaryRepository: CloudinaryRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    private val activityRepository: ActivityRepository,
    private val tripId: String,
    private val date: String,
    private val currentUid: String,
    private val currentUserName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    // Captured off the trip listener below, used only to fill in the activity-log entry
    // (trip name + who's allowed to see it) — never shown directly in this screen's UI.
    private var currentTripName: String = ""
    private var currentMemberIds: List<String> = emptyList()

    init {
        viewModelScope.launch {
            tripRepository.observeTrip(tripId)
                .catch { e -> Log.e("DayDetailViewModel", "Error observing trip: ${e.message}", e) }
                .collect { trip ->
                    _uiState.value = _uiState.value.copy(canEdit = trip?.roleOf(currentUid)?.canEdit() ?: false)
                    currentTripName = trip?.name.orEmpty()
                    currentMemberIds = trip?.memberIds.orEmpty()
                }
        }

        viewModelScope.launch {
            combine(tripRepository.observeDay(tripId, date), tripRepository.observeItems(tripId, date)) { day, items -> day to items }
                .catch { e ->
                    Log.e("DayDetailViewModel", "Error observing day details: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (day, items) ->
                    _uiState.value = _uiState.value.copy(isLoading = false, day = day, items = items)
                }
        }

        viewModelScope.launch {
            _uiState.map { it.day?.hotel?.lat to it.day?.hotel?.lng }
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
        tripRepository.addItem(tripId, date, item.copy(createdBy = currentUid))
        pushNotificationRepository.notifyItineraryChange(tripId, date, item.title, action = "added")
        logActivity(ActivityType.ITEM_ADDED, "${item.title} was added to $date")
    }

    fun updateItem(item: ItineraryItem) = launchCatching {
        tripRepository.updateItem(tripId, date, item, updatedBy = currentUid)
        pushNotificationRepository.notifyItineraryChange(tripId, date, item.title, action = "updated")
        logActivity(ActivityType.ITEM_UPDATED, "${item.title} was updated on $date")
    }

    fun deleteItem(itemId: String) = launchCatching {
        val title = _uiState.value.items.firstOrNull { it.id == itemId }?.title ?: "An item"
        tripRepository.deleteItem(tripId, date, itemId)
        pushNotificationRepository.notifyItineraryChange(tripId, date, title, action = "removed")
        logActivity(ActivityType.ITEM_REMOVED, "$title was removed from $date")
    }

    fun updateHotel(hotel: HotelInfo?) = launchCatching {
        tripRepository.updateHotel(tripId, date, hotel, updatedBy = currentUid)
        pushNotificationRepository.notifyDayChange(tripId, date, what = "Hotel")
        logActivity(ActivityType.HOTEL_UPDATED, "Hotel updated for $date")
    }

    fun updateFlight(flight: FlightInfo?) = launchCatching {
        tripRepository.updateFlight(tripId, date, flight, updatedBy = currentUid)
        pushNotificationRepository.notifyDayChange(tripId, date, what = "Flight")
        logActivity(ActivityType.FLIGHT_UPDATED, if (flight == null) "Flight removed from $date" else "Flight updated for $date")
    }

    fun updateDayNote(note: String) = launchCatching {
        tripRepository.updateDayNote(tripId, date, note, updatedBy = currentUid)
        pushNotificationRepository.notifyDayChange(tripId, date, what = "A note")
        logActivity(ActivityType.DAY_NOTE_UPDATED, "Day note updated for $date")
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

    fun removeAttachment(itemId: String, attachment: Attachment) = launchCatching {
        val current = _uiState.value.items.firstOrNull { it.id == itemId } ?: return@launchCatching
        tripRepository.updateItem(tripId, date, current.copy(attachments = current.attachments.filterNot { it.id == attachment.id }), updatedBy = currentUid)
        pushNotificationRepository.deleteAttachment(tripId, attachment.publicId, attachment.resourceType)
    }

    private fun logActivity(type: ActivityType, message: String) {
        if (currentTripName.isBlank() || currentMemberIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                activityRepository.log(
                    tripId = tripId, tripName = currentTripName, memberIds = currentMemberIds,
                    type = type, message = message, actorUid = currentUid, actorName = currentUserName, date = date
                )
            }
        }
    }

    private fun launchCatching(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Something went wrong")
            }
        }
    }
}