package com.tripro.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tripro.app.data.model.ActivityEntry
import com.tripro.app.data.model.ActivityType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Backs the "Alerts" tab: a feed of recent edits across every trip the user belongs to.
 * Deliberately a plain client-written Firestore subcollection rather than a Cloud
 * Function trigger, for the same Spark-plan reason as the rest of this app's
 * architecture (see README "Why Netlify Functions instead of Firebase Cloud
 * Functions"). Independent of push notifications — this is purely the in-app feed.
 */
class ActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun log(
        tripId: String,
        tripName: String,
        memberIds: List<String>,
        type: ActivityType,
        message: String,
        actorUid: String,
        actorName: String,
        date: String? = null
    ) {
        val entry = ActivityEntry(
            tripId = tripId, tripName = tripName, date = date, type = type,
            message = message, actorUid = actorUid, actorName = actorName, memberIds = memberIds
        )
        firestore.collection("trips").document(tripId).collection("activity").document().set(entry).await()
    }

    /** Most recent activity across every trip [uid] belongs to, newest first. */
    fun observeRecentActivity(uid: String, limit: Long = 50): Flow<List<ActivityEntry>> = callbackFlow {
        val registration = firestore.collectionGroup("activity")
            .whereArrayContains("memberIds", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ActivityRepository", "Error observing activity: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(ActivityEntry::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }
}