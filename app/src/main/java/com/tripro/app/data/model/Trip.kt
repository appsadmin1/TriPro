package com.tripro.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore document: trips/{tripId}
 *
 * `members` maps a Firebase Auth uid -> Role.value ("owner" | "editor" | "viewer").
 * Firestore security rules gate reads/writes on this map, so it is the single
 * source of truth for who can see and edit a trip (see firestore.rules).
 *
 * All fields need defaults for Firestore's no-arg-constructor deserialization.
 */
data class Trip(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val destination: String = "",
    val coverImageUrl: String = "",
    val coverImagePublicId: String = "",
    val coverImageResourceType: String = "",
    val startDate: String = "", // ISO-8601 yyyy-MM-dd
    val endDate: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val members: Map<String, String> = emptyMap(),
    /** Mirrors members.keys — Firestore can't query into map keys, only whereArrayContains
     *  on an actual array, so this is what TripRepository.observeUserTrips() filters on. */
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp
    val createdAt: Date? = null
) {
    /** Convenience: does [uid] have this role or better? Owner > editor > viewer. */
    fun roleOf(uid: String?): Role = Role.fromValue(uid?.let { members[it] })
}
