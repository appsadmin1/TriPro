package com.tripro.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.tripro.app.data.model.ActivityColorPrefs
import com.tripro.app.data.model.DefaultActivityColorHex
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.NotificationPreferences
import com.tripro.app.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val users get() = firestore.collection("users")

    suspend fun ensureUserProfile(user: FirebaseUser) {
        val doc = mapOf(
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: user.email.orEmpty()),
            "photoUrl" to (user.photoUrl?.toString() ?: "")
        )
        users.document(user.uid).set(doc, SetOptions.merge()).await()
    }

    suspend fun findUidByEmail(email: String): String? {
        val snapshot = users.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.id
    }

    /**
     * Applies any pending invites addressed to [user]'s email. Uses a single .update()
     * covering both `members.{uid}` and `memberIds` — a previous version issued two
     * separate batch.update() calls against the SAME trip document in one WriteBatch,
     * which Firestore rejects (one write per document per batch), so this threw on every
     * reconciliation attempt and silently left invited people out of the trip.
     */
    suspend fun reconcilePendingInvites(user: FirebaseUser) {
        val email = user.email ?: return
        val pending = firestore.collectionGroup("pendingInvites")
            .whereEqualTo("email", email.trim().lowercase())
            .get()
            .await()

        for (inviteDoc in pending.documents) {
            val tripRef = inviteDoc.reference.parent.parent ?: continue
            val role = inviteDoc.getString("role") ?: "viewer"
            firestore.runBatch { batch ->
                batch.update(
                    tripRef,
                    mapOf(
                        "members.${user.uid}" to role,
                        "memberIds" to com.google.firebase.firestore.FieldValue.arrayUnion(user.uid)
                    )
                )
                batch.delete(inviteDoc.reference)
            }.await()
        }
    }

    suspend fun getProfiles(uids: List<String>): Map<String, UserProfile> {
        if (uids.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, UserProfile>()
        uids.distinct().chunked(30).forEach { chunk ->
            val snapshot = users.whereIn(FieldPath.documentId(), chunk).get().await()
            for (doc in snapshot.documents) {
                result[doc.id] = UserProfile(
                    uid = doc.id,
                    email = doc.getString("email") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: ""
                )
            }
        }
        return result
    }

    suspend fun registerFcmToken(uid: String, token: String) {
        users.document(uid).set(
            mapOf("fcmTokens" to com.google.firebase.firestore.FieldValue.arrayUnion(token)),
            SetOptions.merge()
        ).await()
    }

    suspend fun unregisterFcmToken(uid: String, token: String) {
        users.document(uid).update(
            "fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token)
        ).await()
    }

    // ---------------------------------------------------- Notification preferences

    fun observeNotificationPreferences(uid: String): Flow<NotificationPreferences> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            @Suppress("UNCHECKED_CAST")
            val map = snap?.get("notificationPrefs") as? Map<String, Any?>
            trySend(
                NotificationPreferences(
                    tripInvites = map?.get("tripInvites") as? Boolean ?: true,
                    itineraryChanges = map?.get("itineraryChanges") as? Boolean ?: true,
                    dayInfoChanges = map?.get("dayInfoChanges") as? Boolean ?: true
                )
            )
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateNotificationPreferences(uid: String, prefs: NotificationPreferences) {
        users.document(uid).set(
            mapOf(
                "notificationPrefs" to mapOf(
                    "tripInvites" to prefs.tripInvites,
                    "itineraryChanges" to prefs.itineraryChanges,
                    "dayInfoChanges" to prefs.dayInfoChanges
                )
            ),
            SetOptions.merge()
        ).await()
    }

    // ---------------------------------------------------------- Activity marker colors

    fun observeActivityColors(uid: String): Flow<ActivityColorPrefs> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            @Suppress("UNCHECKED_CAST")
            val stored = snap?.get("activityColors") as? Map<String, String>
            val hexByKey = MarkerColorKey.entries.associateWith { key ->
                stored?.get(key.name) ?: DefaultActivityColorHex.getValue(key)
            }
            trySend(ActivityColorPrefs(hexByKey))
        }
        awaitClose { registration.remove() }
    }

    /** Dotted field path (same pattern as `members.$uid` in TripRepository) so this only
     *  ever touches one color key, never clobbering the others. */
    suspend fun updateActivityColor(uid: String, key: MarkerColorKey, hex: String) {
        users.document(uid).set(
            mapOf("activityColors.${key.name}" to hex),
            SetOptions.merge()
        ).await()
    }
}