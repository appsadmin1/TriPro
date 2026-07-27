package com.tripro.app.data.model

/**
 * Result of a weather lookup for one trip day. Open-Meteo's free forecast only covers
 * ~16 days out, so [status] tells the UI whether to render real data or a
 * "check back closer to the date" message per the product requirement.
 */
data class DailyWeather(
    val date: String,
    val status: WeatherStatus,
    val weatherCode: Int? = null,
    val tempMaxC: Double? = null,
    val tempMinC: Double? = null,
    val precipitationProbabilityPct: Int? = null
)

enum class WeatherStatus {
    AVAILABLE,
    NOT_YET_AVAILABLE, // date is beyond the forecast horizon
    NO_LOCATION,        // day has no hotel/lat-lng to look up weather for yet
    ERROR
}
