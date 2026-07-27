package com.tripro.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Free, keyless weather forecast: https://open-meteo.com/en/docs
 * Non-commercial free tier: up to 10,000 calls/day, no API key or credit card
 * (see open-meteo.com/en/terms). Forecast horizon is 16 days out — dates beyond that
 * are handled by WeatherRepository as WeatherStatus.NOT_YET_AVAILABLE without even
 * calling this client.
 */
class OpenMeteoClient(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    data class DailyResult(
        val weatherCode: Int?,
        val tempMaxC: Double?,
        val tempMinC: Double?,
        val precipitationProbabilityMax: Int?
    )

    suspend fun fetchDaily(lat: Double, lng: Double, date: String): DailyResult? =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lng" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto" +
                "&start_date=$date&end_date=$date"

            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                parseDaily(body)
            }
        }

    private fun parseDaily(body: String): DailyResult? {
        val json = JSONObject(body)
        if (json.optBoolean("error", false)) return null
        val daily = json.optJSONObject("daily") ?: return null
        val time = daily.optJSONArray("time") ?: return null
        if (time.length() == 0) return null

        fun intAt(key: String): Int? = daily.optJSONArray(key)?.optDouble(0)?.takeUnless { it.isNaN() }?.toInt()
        fun doubleAt(key: String): Double? = daily.optJSONArray(key)?.optDouble(0)?.takeUnless { it.isNaN() }

        return DailyResult(
            weatherCode = intAt("weather_code"),
            tempMaxC = doubleAt("temperature_2m_max"),
            tempMinC = doubleAt("temperature_2m_min"),
            precipitationProbabilityMax = intAt("precipitation_probability_max")
        )
    }
}
