package com.tripro.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tripro.app.data.model.FlightInfo
import com.tripro.app.data.model.HotelInfo
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.Role
import com.tripro.app.data.model.Trip
import com.tripro.app.data.model.TripDay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * All real-time collaboration in TriPro comes from Firestore's addSnapshotListener:
 * every observe* function below wraps one in a callbackFlow, so any device with the
 * trip open recomposes automatically the instant another collaborator writes a change.
 * Firestore also caches writes locally and syncs when connectivity returns, so this
 * gets offline support for free.
 */
class TripRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val trips get() = firestore.collection("trips")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ---------------------------------------------------------------- Trips

    fun observeUserTrips(uid: String): Flow<List<Trip>> = callbackFlow {
        val registration = trips
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TripRepository", "Error observing user trips: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Trip::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    fun observeTrip(tripId: String): Flow<Trip?> = callbackFlow {
        val registration = trips.document(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("TripRepository", "Error observing trip $tripId: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Trip::class.java))
        }
        awaitClose { registration.remove() }
    }

    /**
     * Creates the trip document and one day document per calendar date in range,
     * all in a single atomic batch so the trip never briefly appears with zero days.
     */
    suspend fun createTrip(
        name: String,
        destination: String,
        coverImageUrl: String,
        startDate: LocalDate,
        endDate: LocalDate,
        ownerId: String,
        ownerName: String
    ): String {
        val tripRef = trips.document()
        val trip = Trip(
            id = tripRef.id,
            name = name,
            destination = destination,
            coverImageUrl = coverImageUrl,
            startDate = startDate.format(dateFormatter),
            endDate = endDate.format(dateFormatter),
            ownerId = ownerId,
            ownerName = ownerName,
            members = mapOf(ownerId to Role.OWNER.value),
            memberIds = listOf(ownerId)
        )

        val batch = firestore.batch()
        batch.set(tripRef, trip)

        var date = startDate
        var index = 1
        while (!date.isAfter(endDate)) {
            val dayRef = tripRef.collection("days").document(date.format(dateFormatter))
            batch.set(dayRef, TripDay(date = date.format(dateFormatter), dayIndex = index))
            date = date.plusDays(1)
            index++
        }
        batch.commit().await()
        return tripRef.id
    }

    suspend fun deleteTrip(tripId: String) {
        trips.document(tripId).delete().await()
        // Note: this does not recursively delete subcollections (days/items/pendingInvites) —
        // Firestore doesn't cascade-delete client-side. For production, do this from a Cloud
        // Function trigger, or run the recursive delete from the Firebase console/CLI.
    }

    // ----------------------------------------------------------------- Days

    fun observeDays(tripId: String): Flow<List<TripDay>> = callbackFlow {
        val registration = trips.document(tripId).collection("days")
            .orderBy("dayIndex", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TripRepository", "Error observing days for trip $tripId: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(TripDay::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    fun observeDay(tripId: String, date: String): Flow<TripDay?> = callbackFlow {
        val registration = trips.document(tripId).collection("days").document(date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TripRepository", "Error observing day $date for trip $tripId: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(TripDay::class.java))
            }
        awaitClose { registration.remove() }
    }

    suspend fun updateHotel(tripId: String, date: String, hotel: HotelInfo?, updatedBy: String) {
        trips.document(tripId).collection("days").document(date)
            .set(mapOf("hotel" to hotel, "updatedBy" to updatedBy), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun updateFlight(tripId: String, date: String, flight: FlightInfo?, updatedBy: String) {
        trips.document(tripId).collection("days").document(date)
            .set(mapOf("flight" to flight, "updatedBy" to updatedBy), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun updateDayNote(tripId: String, date: String, note: String, updatedBy: String) {
        trips.document(tripId).collection("days").document(date)
            .update(mapOf("dayNote" to note, "updatedBy" to updatedBy)).await()
    }

    // ------------------------------------------------------- Itinerary items

    fun observeItems(tripId: String, date: String): Flow<List<ItineraryItem>> = callbackFlow {
        val registration = trips.document(tripId).collection("days").document(date)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("TripRepository", "Error observing items for trip $tripId on $date: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(ItineraryItem::class.java).orEmpty()
                    .sortedBy { it.sortMinutes() }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addItem(tripId: String, date: String, item: ItineraryItem): String {
        val ref = trips.document(tripId).collection("days").document(date).collection("items").document()
        ref.set(item.copy(id = ref.id, updatedBy = item.createdBy)).await()
        return ref.id
    }

    suspend fun updateItem(tripId: String, date: String, item: ItineraryItem, updatedBy: String) {
        trips.document(tripId).collection("days").document(date)
            .collection("items").document(item.id).set(item.copy(updatedBy = updatedBy)).await()
    }

    suspend fun deleteItem(tripId: String, date: String, itemId: String) {
        trips.document(tripId).collection("days").document(date)
            .collection("items").document(itemId).delete().await()
    }

    // ------------------------------------------------------------ Members

    suspend fun setMemberRole(tripId: String, uid: String, role: Role) {
        trips.document(tripId).update(
            mapOf(
                "members.$uid" to role.value,
                "memberIds" to FieldValue.arrayUnion(uid)
            )
        ).await()
    }

    suspend fun removeMember(tripId: String, uid: String) {
        trips.document(tripId).update(
            mapOf(
                "members.$uid" to FieldValue.delete(),
                "memberIds" to FieldValue.arrayRemove(uid)
            )
        ).await()
    }

    /** Existing user found immediately -> add them directly. Unknown email -> queue a
     *  pendingInvite (doc ID = the email itself, see firestore.rules for why), reconciled
     *  automatically the first time that person signs in (see UserRepository.reconcilePendingInvites). */
    suspend fun inviteByEmail(tripId: String, email: String, role: Role, invitedBy: String, existingUid: String?) {
        if (existingUid != null) {
            setMemberRole(tripId, existingUid, role)
        } else {
            val normalizedEmail = email.trim().lowercase()
            trips.document(tripId).collection("pendingInvites").document(normalizedEmail).set(
                mapOf(
                    "email" to normalizedEmail,
                    "role" to role.value,
                    "invitedBy" to invitedBy,
                    "invitedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun observePendingInvites(tripId: String): Flow<List<Pair<String, String>>> = callbackFlow {
        // Pair<email, role>, for showing "Invite pending" rows in the Collaborators screen.
        val registration = trips.document(tripId).collection("pendingInvites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val pairs = snapshot?.documents?.mapNotNull { doc ->
                    val email = doc.getString("email") ?: return@mapNotNull null
                    val role = doc.getString("role") ?: Role.VIEWER.value
                    email to role
                }.orEmpty()
                trySend(pairs)
            }
        awaitClose { registration.remove() }
    }
}
