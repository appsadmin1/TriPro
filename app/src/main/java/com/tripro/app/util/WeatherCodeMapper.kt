package com.tripro.app.util

/**
 * WMO weather interpretation codes, per Open-Meteo's docs
 * (https://open-meteo.com/en/docs#weather_variable_documentation).
 *
 * Display text moved to weatherConditionLabel() in DisplayLabels.kt, since that needs a
 * Composable context to resolve stringResource(). This object now only maps codes to
 * Material Symbols glyph names, which aren't user-visible text.
 */
object WeatherCodeMapper {
    /** Material Symbols glyph name to render alongside the weather condition. */
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