package com.tripro.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.tripro.app.BuildConfig
import com.tripro.app.data.model.FlightInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FlightLookupRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = BuildConfig.NETLIFY_FUNCTIONS_BASE_URL
) {
    /** Returns null if the backend isn't configured, the flight isn't found, or the
     *  lookup fails — callers should show a plain "couldn't find that flight" message. */
    suspend fun lookupFlight(tripId: String, flightNumber: String, date: String): FlightInfo? = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || flightNumber.isBlank()) return@withContext null
        runCatching {
            val idToken = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token ?: return@withContext null

            val url = "$baseUrl/.netlify/functions/flight-lookup".toHttpUrl().newBuilder()
                .addQueryParameter("tripId", tripId)
                .addQueryParameter("flightNumber", flightNumber)
                .addQueryParameter("date", date)
                .build()

            val request = Request.Builder().url(url).addHeader("Authorization", "Bearer $idToken").get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                FlightInfo(
                    airline = json.optString("airline"),
                    flightNumber = json.optString("flightNumber", flightNumber),
                    departureAirportCode = json.optString("departureAirportCode"),
                    arrivalAirportCode = json.optString("arrivalAirportCode"),
                    departureAirportLat = json.optDouble("departureAirportLat").takeUnless { it.isNaN() },
                    departureAirportLng = json.optDouble("departureAirportLng").takeUnless { it.isNaN() },
                    arrivalAirportLat = json.optDouble("arrivalAirportLat").takeUnless { it.isNaN() },
                    arrivalAirportLng = json.optDouble("arrivalAirportLng").takeUnless { it.isNaN() },
                    departureTime = json.optString("departureTime"),
                    arrivalTime = json.optString("arrivalTime")
                )
            }
        }.getOrNull()
    }
}