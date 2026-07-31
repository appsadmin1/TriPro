package com.tripro.app.ui.auth

import android.util.Log
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
                    // Show the trips list immediately — profile sync, invite reconciliation,
                    // and push-token registration are best-effort background work and must
                    // never delay getting an already-logged-in user to the app.
                    _uiState.value = AuthUiState.SignedIn(user)
                    launch { syncAfterSignIn(user) }
                } else {
                    _uiState.value = AuthUiState.SignedOut
                }
            }
        }
    }

    fun signIn() {
        _uiState.value = AuthUiState.SigningIn()
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithGoogle()
                result.onSuccess { user ->
                    _uiState.value = AuthUiState.SignedIn(user)
                    launch { syncAfterSignIn(user) }
                }.onFailure { e ->
                    val message = e.message ?: "Sign-in failed"
                    Log.e("AuthViewModel", "Sign-in failure: $message", e)
                    _uiState.value = AuthUiState.SigningIn(error = message)
                }
            } catch (e: Throwable) {
                val message = e.message ?: "Unexpected sign-in error"
                Log.e("AuthViewModel", "Fatal error during sign-in", e)
                _uiState.value = AuthUiState.SigningIn(error = message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
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

    private suspend fun syncAfterSignIn(user: FirebaseUser) {
        runCatching { userRepository.ensureUserProfile(user) }
            .onFailure { Log.e("AuthViewModel", "Failed to update user profile", it) }
        runCatching { userRepository.reconcilePendingInvites(user) }
            .onFailure { Log.e("AuthViewModel", "Failed to reconcile invites", it) }
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            userRepository.registerFcmToken(user.uid, token)
        }.onFailure { Log.e("AuthViewModel", "Failed to register push token", it) }
    }
}