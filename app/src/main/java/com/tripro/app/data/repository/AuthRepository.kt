package com.tripro.app.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.tripro.app.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/**
 * Wraps sign-in with Google via Jetpack Credential Manager, then exchanges the
 * resulting Google ID token for a Firebase Auth session.
 *
 * GoogleSignInClient (com.google.android.gms.auth.api.signin) is deprecated by Google
 * in favor of Credential Manager — see https://developer.android.com/identity/sign-in/credential-manager-siwg
 *
 * Requires BuildConfig.WEB_CLIENT_ID: the *Web* OAuth client ID from your Firebase
 * project (Authentication > Sign-in method > Google > Web SDK configuration), not the
 * Android client ID. Wired up via WEB_CLIENT_ID in local.properties — see README.
 */
class AuthRepository(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /** Emits on every Firebase auth state change, including the initial state. */
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(): Result<FirebaseUser> = try {
        if (BuildConfig.WEB_CLIENT_ID.isEmpty()) {
            throw IllegalStateException("WEB_CLIENT_ID is missing from local.properties. See local.properties.example.")
        }

        val nonce = generateNonce()
        val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        Log.d("AuthRepository", "Starting Credential Manager request...")
        val credentialManager = CredentialManager.create(context)
        val response = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)

        Log.d("AuthRepository", "Exchanging Google token for Firebase credential...")
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
        val user = authResult.user ?: return Result.failure(IllegalStateException("No user returned from Firebase"))
        Result.success(user)
    } catch (e: GetCredentialException) {
        Log.e("AuthRepository", "Credential Manager error: ${e.type}", e)
        Result.failure(e)
    } catch (e: Throwable) {
        Log.e("AuthRepository", "Unexpected error during sign-in", e)
        Result.failure(e)
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Best-effort: clearing credential state failing shouldn't block sign-out.
        }
    }

    private fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
