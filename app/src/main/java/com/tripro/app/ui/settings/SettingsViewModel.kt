package com.tripro.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.ActivityColorPrefs
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.NotificationPreferences
import com.tripro.app.data.repository.AuthRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val notificationPrefs: NotificationPreferences = NotificationPreferences(),
    val activityColors: ActivityColorPrefs = ActivityColorPrefs()
)

class SettingsViewModel(
    private val userRepository: UserRepository,
    authRepository: AuthRepository,
    private val currentUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val user = authRepository.currentUser
        _uiState.value = _uiState.value.copy(
            displayName = user?.displayName ?: user?.email.orEmpty(),
            email = user?.email.orEmpty(),
            photoUrl = user?.photoUrl?.toString().orEmpty()
        )
        viewModelScope.launch {
            combine(
                userRepository.observeNotificationPreferences(currentUid),
                userRepository.observeActivityColors(currentUid)
            ) { prefs, colors -> prefs to colors }
                .collect { (prefs, colors) ->
                    _uiState.value = _uiState.value.copy(isLoading = false, notificationPrefs = prefs, activityColors = colors)
                }
        }
    }

    fun setTripInvites(enabled: Boolean) = updatePrefs { it.copy(tripInvites = enabled) }
    fun setItineraryChanges(enabled: Boolean) = updatePrefs { it.copy(itineraryChanges = enabled) }
    fun setDayInfoChanges(enabled: Boolean) = updatePrefs { it.copy(dayInfoChanges = enabled) }

    private fun updatePrefs(transform: (NotificationPreferences) -> NotificationPreferences) {
        val updated = transform(_uiState.value.notificationPrefs)
        _uiState.value = _uiState.value.copy(notificationPrefs = updated)
        viewModelScope.launch { userRepository.updateNotificationPreferences(currentUid, updated) }
    }

    fun setActivityColor(key: MarkerColorKey, hex: String) {
        _uiState.value = _uiState.value.copy(
            activityColors = ActivityColorPrefs(_uiState.value.activityColors.hexByKey + (key to hex))
        )
        viewModelScope.launch { userRepository.updateActivityColor(currentUid, key, hex) }
    }
}