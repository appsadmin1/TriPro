package com.tripro.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.tripro.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * TriPro's Cloud Functions were replaced with plain Netlify Functions (see
 * netlify/functions/) so the Firebase project never needs the Blaze plan. Netlify
 * Functions aren't Firestore-triggered the way Cloud Functions were, so the app itself
 * calls these endpoints right after a write succeeds — see the call sites in
 * CollaboratorsViewModel and DayDetailViewModel.
 *
 * Every call sends the caller's Firebase ID token so the function can verify who's
 * asking (never trust a uid passed in the request body) and confirm they're actually a
 * member of the trip before sending anything or deleting anything — see
 * netlify/functions/_shared/verifyTripMember.mjs.
 *
 * Deliberately fire-and-forget: notifications and attachment cleanup are best-effort
 * side effects, not the source of truth (Firestore is), so a network hiccup here should
 * never surface as an error to the person who just successfully saved something.
 */
class PushNotificationRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = BuildConfig.NETLIFY_FUNCTIONS_BASE_URL
) {
    suspend fun notifyTripInvite(tripId: String, invitedUid: String) {
        post("notify", JSONObject().apply {
            put("type", "trip_invite")
            put("tripId", tripId)
            put("invitedUid", invitedUid)
        })
    }

    suspend fun notifyItineraryChange(tripId: String, date: String, itemTitle: String, action: String) {
        post("notify", JSONObject().apply {
            put("type", "itinerary_update")
            put("tripId", tripId)
            put("date", date)
            put("itemTitle", itemTitle)
            put("action", action) // "added" | "updated" | "removed"
        })
    }

    suspend fun notifyDayChange(tripId: String, date: String, what: String) {
        post("notify", JSONObject().apply {
            put("type", "day_update")
            put("tripId", tripId)
            put("date", date)
            put("what", what) // "Hotel" | "Flight" | "A note"
        })
    }

    suspend fun deleteAttachment(tripId: String, date: String, itemId: String, attachmentId: String) =
        post("delete-attachment", JSONObject().apply {
            put("tripId", tripId)
            put("date", date)
            put("itemId", itemId)
            put("attachmentId", attachmentId)
        })

    suspend fun deleteItem(tripId: String, date: String, itemId: String): Result<Unit> {
        return post("delete-item", JSONObject().apply {
            put("tripId", tripId)
            put("date", date)
            put("itemId", itemId)
        })
    }

    suspend fun deleteTrip(tripId: String): Result<Unit> {
        return post("delete-trip", JSONObject().apply {
            put("tripId", tripId)
        })
    }

    suspend fun deleteCloudinaryAsset(tripId: String, publicId: String, resourceType: String): Result<Unit> {
        return post("delete-cloudinary-asset", JSONObject().apply {
            put("tripId", tripId)
            put("publicId", publicId)
            put("resourceType", resourceType)
        })
    }

    suspend fun notifyTripUpdate(tripId: String, what: String) {
        post("notify", JSONObject().apply {
            put("type", "trip_update")
            put("tripId", tripId)
            put("what", what)
        })
    }

    private suspend fun post(functionName: String, body: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (baseUrl.isBlank()) return@runCatching // notifications backend not configured — skip silently
            val idToken = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
                ?: return@runCatching

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/.netlify/functions/$functionName")
                .addHeader("Authorization", "Bearer $idToken")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw Exception("Netlify function $functionName failed: ${response.code} $errorBody")
                }
            }
        }
    }
}
