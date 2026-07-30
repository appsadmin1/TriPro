package com.tripro.app.ui.alerts

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.ActivityEntry
import com.tripro.app.data.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AlertsUiState(
    val isLoading: Boolean = true,
    val entries: List<ActivityEntry> = emptyList(),
    val hasUnread: Boolean = false
)

/** Same reasoning as SearchTripsViewModel: created at the nav-graph level so the
 *  bottom-nav badge is live everywhere, so this listener also starts the instant
 *  someone signs in and must never crash the app — worst case the badge/feed just
 *  stays empty until whatever's wrong (rules, connectivity, ...) is fixed. */
class AlertsViewModel(
    private val activityRepository: ActivityRepository,
    private val currentUid: String,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            activityRepository.observeRecentActivity(currentUid)
                .catch { e ->
                    Log.e("AlertsViewModel", "Error observing activity: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { entries ->
                    val lastSeen = prefs.getLong(PREF_LAST_SEEN, 0L)
                    val newest = entries.firstOrNull()?.createdAt?.time ?: 0L
                    _uiState.value = _uiState.value.copy(isLoading = false, entries = entries, hasUnread = newest > lastSeen)
                }
        }
    }

    fun markAllSeen() {
        val newest = _uiState.value.entries.firstOrNull()?.createdAt?.time ?: System.currentTimeMillis()
        prefs.edit().putLong(PREF_LAST_SEEN, newest).apply()
        _uiState.value = _uiState.value.copy(hasUnread = false)
    }

    companion object {
        private const val PREF_LAST_SEEN = "alerts_last_seen_millis"
    }
}