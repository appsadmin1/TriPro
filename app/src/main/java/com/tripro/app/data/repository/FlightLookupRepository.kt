package com.tripro.app.data.repository

import android.util.Log
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
import java.io.IOException

data class FlightLookupResult(
    val airline: String,
    val flightNumber: String,
    val departureAirportCode: String,
    val departureAirportLat: Double?,
    val departureAirportLng: Double?,
    val departureTime: String, // "HH:mm" local to the departure airport
    val arrivalAirportCode: String,
    val arrivalAirportLat: Double?,
    val arrivalAirportLng: Double?,
    val arrivalTime: String // "HH:mm" local to the arrival airport
)

/**
 * Looks up a scheduled flight by flight number + date via AeroDataBox (RapidAPI),
 * proxied through a Netlify Function so the RapidAPI key never ships in the APK — same
 * reasoning as CloudinaryRepository/PushNotificationRepository and the Cloudinary API
 * Secret. See netlify/functions/flight-lookup.mjs.
 */
class FlightLookupRepository(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val baseUrl: String = BuildConfig.NETLIFY_FUNCTIONS_BASE_URL
) {
    private val tag = "FlightLookupRepository"
    suspend fun lookup(flightNumber: String, date: String): Result<FlightLookupResult> = withContext(Dispatchers.IO) {
        try {
            if (baseUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Flight lookup isn't configured (NETLIFY_FUNCTIONS_BASE_URL missing)."))
            }
            val idToken = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
                ?: return@withContext Result.failure(IllegalStateException("You need to be signed in to look up a flight."))

            val requestBody = JSONObject().apply {
                put("flightNumber", flightNumber)
                put("date", date)
            }.toString().toRequestBody("application/json".toMediaType())

            val url = baseUrl.trimEnd('/') + "/.netlify/functions/flight-lookup"
            Log.d(tag, "Looking up flight: $flightNumber on $date at $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $idToken")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseText = response.body?.string() ?: ""
                Log.d(tag, "Flight lookup response [${response.code}]")
                
                if (!response.isSuccessful) {
                    val detail = if (responseText.isNotBlank()) {
                        runCatching { JSONObject(responseText).optString("error") }.getOrNull() ?: "Error ${response.code}"
                    } else "HTTP ${response.code}"
                    
                    Log.e(tag, "flight-lookup request failed: $detail")
                    return@withContext Result.failure(IOException(detail))
                }
                
                if (responseText.isBlank()) throw IOException("Empty response from server")
                val flight = JSONObject(responseText).getJSONObject("flight")
                Result.success(parseFlight(flight))
            }
        } catch (e: Exception) {
            Log.e(tag, "flight-lookup threw", e)
            Result.failure(e)
        }
    }

    private fun parseFlight(flight: JSONObject): FlightLookupResult {
        val departure = flight.getJSONObject("departure")
        val arrival = flight.getJSONObject("arrival")
        val departureAirport = departure.getJSONObject("airport")
        val arrivalAirport = arrival.getJSONObject("airport")
        val airline = flight.optJSONObject("airline")

        return FlightLookupResult(
            airline = airline?.optString("name").orEmpty(),
            flightNumber = flight.optString("number").trim(),
            departureAirportCode = departureAirport.optString("iata").uppercase(),
            departureAirportLat = latLngOf(departureAirport, "lat"),
            departureAirportLng = latLngOf(departureAirport, "lon"),
            departureTime = localTimeOf(departure.optJSONObject("scheduledTime")?.optString("local")),
            arrivalAirportCode = arrivalAirport.optString("iata").uppercase(),
            arrivalAirportLat = latLngOf(arrivalAirport, "lat"),
            arrivalAirportLng = latLngOf(arrivalAirport, "lon"),
            arrivalTime = localTimeOf(arrival.optJSONObject("scheduledTime")?.optString("local"))
        )
    }

    private fun latLngOf(airport: JSONObject, key: String): Double? =
        airport.optJSONObject("location")?.optDouble(key)?.takeUnless { it.isNaN() }

    /** AeroDataBox's "local" timestamps look like "2026-08-02 15:35+02:00" — this pulls
     *  just the HH:mm portion, which is all TriPro's FlightInfo stores. */
    private fun localTimeOf(local: String?): String {
        if (local.isNullOrBlank()) return ""
        return local.substringAfter(" ", "").take(5)
    }
}