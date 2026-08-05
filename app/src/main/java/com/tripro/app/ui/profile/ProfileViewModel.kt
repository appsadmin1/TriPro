package com.tripro.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripro.app.data.model.UserProfile
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val isEditing: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val uid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profiles = userRepository.getProfiles(listOf(uid))
                _uiState.value = _uiState.value.copy(isLoading = false, profile = profiles[uid])
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun startEditing() {
        _uiState.value = _uiState.value.copy(isEditing = true)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(isEditing = false)
    }

    fun updateProfile(name: String, photoUrl: String) {
        viewModelScope.launch {
            try {
                userRepository.updateUserProfile(uid, name, photoUrl)
                _uiState.value = _uiState.value.copy(
                    isEditing = false,
                    profile = _uiState.value.profile?.copy(displayName = name, photoUrl = photoUrl)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
