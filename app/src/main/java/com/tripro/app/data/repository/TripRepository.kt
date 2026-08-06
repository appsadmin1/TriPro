package com.tripro.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.TripDay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TripRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val trips get() = firestore.collection("trips")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun observeUserTrips(uid: String): Flow<List<Trip>> = callbackFlow {
        Log.d("TripRepository", "Observing trips for uid: $uid")
        val registration = trips.whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TripRepository", "Error observing user trips: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                val result = snapshot?.toObjects(Trip::class.java).orEmpty()
                Log.d("TripRepository", "Found ${result.size} trips for uid: $uid")
                trySend(result)
            }
        awaitClose { registration.remove() }
    }

    fun observeTrip(tripId: String): Flow<Trip?> = callbackFlow {
        val registration = trips.document(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) { Log.e("TripRepository", "Error observing trip $tripId: ${error.message}", error); close(error); return@addSnapshotListener }
            trySend(snapshot?.toObject(Trip::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun createTrip(
        name: String, destination: String, coverImageUrl: String,
        coverImagePublicId: String, coverImageResourceType: String,
        startDate: LocalDate, endDate: LocalDate, ownerId: String, ownerName: String
    ): String {
        val tripRef = trips.document()
        val trip = Trip(
            id = tripRef.id, name = name, destination = destination, 
            coverImageUrl = coverImageUrl, coverImagePublicId = coverImagePublicId,
            coverImageResourceType = coverImageResourceType,
            startDate = startDate.format(dateFormatter), endDate = endDate.format(dateFormatter),
            ownerId = ownerId, ownerName = ownerName,
            members = mapOf(ownerId to Role.OWNER.value), memberIds = listOf(ownerId)
        )
        val batch = firestore.batch()
        batch.set(tripRef, trip)
        var date = startDate
        var index = 1
        while (!date.isAfter(endDate)) {
            val dayRef = tripRef.collection("days").document(date.format(dateFormatter))
            batch.set(dayRef, TripDay(date = date.format(dateFormatter), dayIndex = index))
            date = date.plusDays(1); index++
        }
        batch.commit().await()
        return tripRef.id
    }

    /** Updates name/destination/dates/(optionally) cover photo, and reconciles the `days`
     *  subcollection to the new range. A shorter range leaves day docs (and any items
     *  under them) outside the new range in place rather than deleting them — Firestore
     *  can't cascade-delete subcollections client-side (same limitation as deleteTrip). */
    suspend fun updateTripDetails(
        tripId: String, name: String, destination: String, 
        coverImageUrl: String?, coverImagePublicId: String?, coverImageResourceType: String?,
        startDate: LocalDate, endDate: LocalDate
    ) {
        val tripRef = trips.document(tripId)
        val existingDates = tripRef.collection("days").get().await().documents.map { it.id }.toSet()

        val batch = firestore.batch()
        val updateMap = mutableMapOf<String, Any>(
            "name" to name, "destination" to destination,
            "startDate" to startDate.format(dateFormatter), "endDate" to endDate.format(dateFormatter)
        )
        if (coverImageUrl != null) {
            updateMap["coverImageUrl"] = coverImageUrl
            updateMap["coverImagePublicId"] = coverImagePublicId.orEmpty()
            updateMap["coverImageResourceType"] = coverImageResourceType.orEmpty()
        }
        batch.update(tripRef, updateMap)

        var date = startDate
        var index = 1
        while (!date.isAfter(endDate)) {
            val dateStr = date.format(dateFormatter)
            val dayRef = tripRef.collection("days").document(dateStr)
            if (dateStr in existingDates) batch.update(dayRef, "dayIndex", index)
            else batch.set(dayRef, TripDay(date = dateStr, dayIndex = index))
            date = date.plusDays(1); index++
        }
        batch.commit().await()
    }

    suspend fun deleteTrip(tripId: String) {
        trips.document(tripId).delete().await()
    }

    fun observeDays(tripId: String): Flow<List<TripDay>> = callbackFlow {
        val registration = trips.document(tripId).collection("days")
            .orderBy("dayIndex", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("TripRepository", "Error observing days for trip $tripId: ${error.message}", error); close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(TripDay::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    fun observeDay(tripId: String, date: String): Flow<TripDay?> = callbackFlow {
        val registration = trips.document(tripId).collection("days").document(date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("TripRepository", "Error observing day $date for trip $tripId: ${error.message}", error); close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(TripDay::class.java))
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateDayNote(tripId: String, date: String, note: String, updatedBy: String) {
        trips.document(tripId).collection("days").document(date).update(mapOf("dayNote" to note, "updatedBy" to updatedBy)).await()
    }

    fun observeItems(tripId: String, date: String): Flow<List<ItineraryItem>> = callbackFlow {
        val registration = trips.document(tripId).collection("days").document(date).collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e("TripRepository", "Error observing items for trip $tripId on $date: ${error.message}", error); close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(ItineraryItem::class.java).orEmpty().sortedBy { it.sortMinutes() })
            }
        awaitClose { registration.remove() }
    }

    /** One listener per known day (not a collectionGroup query) — reuses the exact same
     *  per-day/tripId authorization as observeItems, for the "docs grouped by date" view. */
    fun observeAllItemsForTrip(tripId: String, dates: List<String>): Flow<Map<String, List<ItineraryItem>>> {
        if (dates.isEmpty()) return flowOf(emptyMap())
        
        val flows = dates.map { date ->
            observeItems(tripId, date).map { date to it }
        }
        
        return combine(flows) { array ->
            array.toMap()
        }
    }

    suspend fun addItem(tripId: String, date: String, item: ItineraryItem): String {
        val ref = trips.document(tripId).collection("days").document(date).collection("items").document()
        ref.set(item.copy(id = ref.id, updatedBy = item.createdBy)).await()
        return ref.id
    }

    suspend fun updateItem(tripId: String, date: String, item: ItineraryItem, updatedBy: String) {
        trips.document(tripId).collection("days").document(date).collection("items").document(item.id).set(item.copy(updatedBy = updatedBy)).await()
    }

    suspend fun deleteItem(tripId: String, date: String, itemId: String) {
        trips.document(tripId).collection("days").document(date).collection("items").document(itemId).delete().await()
    }

    /** Renames one attachment's display name in place — Cloudinary's asset id never
     *  changes, so this only ever touches the Firestore-side display name. */
    suspend fun renameAttachment(tripId: String, date: String, itemId: String, attachmentId: String, newName: String) {
        val itemRef = trips.document(tripId).collection("days").document(date).collection("items").document(itemId)
        val item = itemRef.get().await().toObject(ItineraryItem::class.java) ?: return
        val updatedAttachments = item.attachments.map { if (it.id == attachmentId) it.copy(fileName = newName) else it }
        itemRef.update("attachments", updatedAttachments).await()
    }

    suspend fun setMemberRole(tripId: String, uid: String, role: Role) {
        trips.document(tripId).update(mapOf("members.$uid" to role.value, "memberIds" to FieldValue.arrayUnion(uid))).await()
    }

    suspend fun removeMember(tripId: String, uid: String) {
        trips.document(tripId).update(mapOf("members.$uid" to FieldValue.delete(), "memberIds" to FieldValue.arrayRemove(uid))).await()
    }

    suspend fun inviteByEmail(tripId: String, email: String, role: Role, invitedBy: String, existingUid: String?) {
        if (existingUid != null) {
            setMemberRole(tripId, existingUid, role)
        } else {
            val normalizedEmail = email.trim().lowercase()
            trips.document(tripId).collection("pendingInvites").document(normalizedEmail).set(
                mapOf("email" to normalizedEmail, "role" to role.value, "invitedBy" to invitedBy, "invitedAt" to FieldValue.serverTimestamp())
            ).await()
        }
    }

    fun observePendingInvites(tripId: String): Flow<List<Pair<String, String>>> = callbackFlow {
        val registration = trips.document(tripId).collection("pendingInvites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents?.mapNotNull { doc ->
                    val email = doc.getString("email") ?: return@mapNotNull null
                    email to (doc.getString("role") ?: Role.VIEWER.value)
                }.orEmpty())
            }
        awaitClose { registration.remove() }
    }
}