package com.tripro.app.util

/**
 * WMO weather interpretation codes, per Open-Meteo's docs
 * (https://open-meteo.com/en/docs#weather_variable_documentation).
 */
object WeatherCodeMapper {
    fun label(code: Int?): String = when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Weather"
    }

    /** Material Symbols glyph name to render alongside [label]. */
    fun icon(code: Int?): String = when (code) {
        0 -> "clear_day"
        1, 2 -> "partly_cloudy_day"
        3 -> "cloud"
        45, 48 -> "foggy"
        51, 53, 55, 56, 57 -> "rainy_light"
        61, 63, 65, 66, 67, 80, 81, 82 -> "rainy"
        71, 73, 75, 77, 85, 86 -> "weather_snowy"
        95, 96, 99 -> "thunderstorm"
        else -> "partly_cloudy_day"
    }
}
