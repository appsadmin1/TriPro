package com.tripro.app.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.tripro.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await

/**
 * Firestore document: users/{uid} — { email, displayName, photoUrl }
 *
 * This tiny public directory exists for exactly one reason: the client SDK cannot look
 * up "which uid owns this email" against Firebase Auth directly, but collaborator invites
 * are entered by email. Writing a profile doc on every login gives us something to query.
 *
 * Security rule: any signed-in user may read users/*, but may only write their own doc
 * (see firestore.rules) — so this can't be used to enumerate arbitrary account data beyond
 * what a user already chose to expose.
 */
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
        // merge = true: don't clobber anything else we might store on this doc later
        users.document(user.uid).set(doc, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /** Returns the uid for [email], or null if nobody with that email has ever signed in. */
    suspend fun findUidByEmail(email: String): String? {
        val snapshot = users.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.id
    }

    /**
     * Applies any pending invites addressed to [user]'s email — i.e. invites created before
     * that person ever signed in. Call this once right after login. Uses a collectionGroup
     * query across every trip's pendingInvites subcollection, scoped by security rules to
     * only the caller's own email (see firestore.rules).
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
                batch.update(tripRef, "members.${user.uid}", role)
                batch.update(tripRef, "memberIds", com.google.firebase.firestore.FieldValue.arrayUnion(user.uid))
                batch.delete(inviteDoc.reference)
            }.await()
        }
    }

    suspend fun getProfiles(uids: List<String>): Map<String, UserProfile> {
        if (uids.isEmpty()) return emptyMap()
        // Firestore whereIn supports at most 30 values per query.
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

    /**
     * Adds an FCM registration token to users/{uid}.fcmTokens (a plain string array —
     * arrayUnion so multiple devices per person just accumulate distinct tokens). Called
     * both right after login (TriProApplication) and whenever FCM rotates the token
     * (TriProMessagingService.onNewToken).
     */
    suspend fun registerFcmToken(uid: String, token: String) {
        users.document(uid).set(
            mapOf("fcmTokens" to com.google.firebase.firestore.FieldValue.arrayUnion(token)),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    /** Called on sign-out so a shared/borrowed device stops receiving this user's pushes. */
    suspend fun unregisterFcmToken(uid: String, token: String) {
        users.document(uid).update(
            "fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token)
        ).await()
    }
}
