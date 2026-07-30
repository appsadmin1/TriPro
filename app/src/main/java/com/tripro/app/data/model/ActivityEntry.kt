package com.tripro.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class ActivityType {
    ITEM_ADDED, ITEM_UPDATED, ITEM_REMOVED,
    HOTEL_UPDATED, FLIGHT_UPDATED, DAY_NOTE_UPDATED,
    MEMBER_INVITED, MEMBER_ROLE_CHANGED, MEMBER_REMOVED
}

/** Firestore document: trips/{tripId}/activity/{entryId} — backs the "Alerts" tab. */
data class ActivityEntry(
    @DocumentId val id: String = "",
    val tripId: String = "",
    val tripName: String = "",
    val date: String? = null, // yyyy-MM-dd, when the change is day/item-scoped
    val type: ActivityType = ActivityType.ITEM_UPDATED,
    val message: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    /** Snapshot of trip.memberIds at write time, so this one document is enough for
     *  both the client's collectionGroup query and firestore.rules to authorize —
     *  no second read of the parent trip needed (same trick as pendingInvites). */
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null
)