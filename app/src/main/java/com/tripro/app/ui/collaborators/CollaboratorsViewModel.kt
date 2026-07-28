package com.tripro.app.ui.collaborators

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.UserProfile
import com.tripro.app.data.repository.PushNotificationRepository
import com.tripro.app.data.repository.TripRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MemberRow(val profile: UserProfile, val role: Role)

data class CollaboratorsUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val members: List<MemberRow> = emptyList(),
    val pendingInvites: List<Pair<String, String>> = emptyList(), // email, role
    val isOwner: Boolean = false,
    val inviteError: String? = null,
    val inviteSuccessMessage: String? = null
)

class CollaboratorsViewModel(
    private val tripRepository: TripRepository,
    private val userRepository: UserRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    private val tripId: String,
    private val currentUid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollaboratorsUiState())
    val uiState: StateFlow<CollaboratorsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tripRepository.observeTrip(tripId),
                tripRepository.observePendingInvites(tripId)
            ) { trip, pending -> trip to pending }
                .catch { e ->
                    Log.e("CollaboratorsViewModel", "Error observing collaborator data: ${e.message}", e)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { (trip, pending) ->
                if (trip == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@collect
                }
                val profiles = userRepository.getProfiles(trip.memberIds)
                val members = trip.memberIds.mapNotNull { uid ->
                    profiles[uid]?.let { MemberRow(it, trip.roleOf(uid)) }
                }.sortedByDescending { it.role == Role.OWNER }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    trip = trip,
                    members = members,
                    pendingInvites = pending,
                    isOwner = trip.roleOf(currentUid) == Role.OWNER
                )
            }
        }
    }

    fun invite(email: String, role: Role) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            _uiState.value = _uiState.value.copy(inviteError = "Enter a valid email address.")
            return
        }
        viewModelScope.launch {
            try {
                val existingUid = userRepository.findUidByEmail(trimmed)
                tripRepository.inviteByEmail(tripId, trimmed, role, currentUid, existingUid)
                if (existingUid != null) {
                    pushNotificationRepository.notifyTripInvite(tripId, existingUid)
                }
                _uiState.value = _uiState.value.copy(
                    inviteError = null,
                    inviteSuccessMessage = if (existingUid != null) "Added $trimmed as ${role.value}."
                    else "Invite sent — $trimmed will be added once they sign in to TriPro."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(inviteError = e.message ?: "Couldn't send the invite.")
            }
        }
    }

    fun changeRole(uid: String, role: Role) = viewModelScope.launch {
        tripRepository.setMemberRole(tripId, uid, role)
    }

    fun removeMember(uid: String) = viewModelScope.launch {
        tripRepository.removeMember(tripId, uid)
    }

    fun dismissMessages() {
        _uiState.value = _uiState.value.copy(inviteError = null, inviteSuccessMessage = null)
    }
}
