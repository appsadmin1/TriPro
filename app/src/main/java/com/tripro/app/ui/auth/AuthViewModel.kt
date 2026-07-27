package com.tripro.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.FirebaseMessaging
import com.tripro.app.data.repository.AuthRepository
import com.tripro.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object CheckingSession : AuthUiState
    data object SignedOut : AuthUiState
    data class SigningIn(val error: String? = null) : AuthUiState
    data class SignedIn(val user: FirebaseUser) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState().collect { user ->
                if (user != null) {
                    onUserAuthenticated(user)
                } else {
                    _uiState.value = AuthUiState.SignedOut
                }
            }
        }
    }

    fun signIn() {
        _uiState.value = AuthUiState.SigningIn()
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle()
            result.onSuccess { user -> onUserAuthenticated(user) }
                .onFailure { e -> _uiState.value = AuthUiState.SigningIn(error = e.message ?: "Sign-in failed") }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Best-effort: stop this device from receiving pushes for the account we're
            // leaving. Not fatal if it fails (e.g. offline) — a stale token just means an
            // extra no-op send next time Cloud Functions tries it.
            val uid = authRepository.currentUser?.uid
            if (uid != null) {
                runCatching {
                    val token = FirebaseMessaging.getInstance().token.await()
                    userRepository.unregisterFcmToken(uid, token)
                }
            }
            authRepository.signOut()
            _uiState.value = AuthUiState.SignedOut
        }
    }

    private suspend fun onUserAuthenticated(user: FirebaseUser) {
        // Keep the public users/{uid} directory doc fresh, then apply any collaborator
        // invites that were sent to this email before the person ever signed in.
        userRepository.ensureUserProfile(user)
        userRepository.reconcilePendingInvites(user)
        registerPushToken(user.uid)
        _uiState.value = AuthUiState.SignedIn(user)
    }

    /** Fetches the current FCM token and stores it — covers the case where a token was
     *  already generated before this device ever signed in (onNewToken only fires on
     *  token creation/rotation, not on every login). */
    private suspend fun registerPushToken(uid: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepository.registerFcmToken(uid, token)
        }
    }
}
