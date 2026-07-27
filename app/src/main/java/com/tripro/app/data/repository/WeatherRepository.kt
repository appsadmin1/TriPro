package com.tripro.app.data.repository

import com.tripro.app.data.model.DailyWeather
import com.tripro.app.data.model.WeatherStatus
import com.tripro.app.data.remote.OpenMeteoClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Open-Meteo's free forecast covers today .. today+16 days, plus up to 92 days in the past. */
private const val FORECAST_HORIZON_DAYS = 16
private const val PAST_HORIZON_DAYS = 92

class WeatherRepository(
    private val client: OpenMeteoClient = OpenMeteoClient()
) {
    suspend fun getDailyWeather(lat: Double?, lng: Double?, date: String): DailyWeather {
        if (lat == null || lng == null) {
            return DailyWeather(date = date, status = WeatherStatus.NO_LOCATION)
        }

        val targetDate = runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }
            .getOrNull() ?: return DailyWeather(date = date, status = WeatherStatus.ERROR)

        val daysFromToday = ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
        if (daysFromToday > FORECAST_HORIZON_DAYS || daysFromToday < -PAST_HORIZON_DAYS) {
            return DailyWeather(date = date, status = WeatherStatus.NOT_YET_AVAILABLE)
        }

        val result = client.fetchDaily(lat, lng, date)
            ?: return DailyWeather(date = date, status = WeatherStatus.ERROR)

        return DailyWeather(
            date = date,
            status = WeatherStatus.AVAILABLE,
            weatherCode = result.weatherCode,
            tempMaxC = result.tempMaxC,
            tempMinC = result.tempMinC,
            precipitationProbabilityPct = result.precipitationProbabilityMax
        )
    }

    /** Human-readable date the forecast will unlock, for the "check back on ..." message. */
    fun forecastAvailableFrom(date: String): String {
        val targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        val unlockDate = targetDate.minusDays(FORECAST_HORIZON_DAYS.toLong())
        return unlockDate.format(DateTimeFormatter.ofPattern("MMM d"))
    }
}
