package com.tripro.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tripro.app.R
import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.MarkerColorKey
import com.tripro.app.data.model.Role

/**
 * Composable-only display-name mappers for enums whose model-level `.name`/stored
 * value must stay stable (Firestore, sort logic) even though the *shown* text needs to
 * follow the current app language. Each of these needs a Composable context for
 * stringResource()/pluralStringResource(), so they live here rather than as a plain
 * property on the enum itself (contrast e.g. Role.value, which is a Firestore identifier
 * and must never change).
 */

@Composable
fun ItemType.localizedLabel(): String = when (this) {
    ItemType.FLIGHT -> stringResource(R.string.item_type_flight)
    ItemType.HOTEL -> stringResource(R.string.item_type_hotel)
    ItemType.RESTAURANT -> stringResource(R.string.item_type_restaurant)
    ItemType.ATTRACTION -> stringResource(R.string.item_type_attraction)
    ItemType.ACTIVITY -> stringResource(R.string.item_type_activity)
    ItemType.TRANSPORT -> stringResource(R.string.item_type_transport)
    ItemType.SHOW -> stringResource(R.string.item_type_show)
    ItemType.CUSTOM -> stringResource(R.string.item_type_custom)
}

@Composable
fun DayPeriod.localizedLabel(): String = when (this) {
    DayPeriod.MORNING -> stringResource(R.string.period_morning)
    DayPeriod.NOON -> stringResource(R.string.period_noon)
    DayPeriod.AFTERNOON -> stringResource(R.string.period_afternoon)
    DayPeriod.EVENING -> stringResource(R.string.period_evening)
    DayPeriod.NIGHT -> stringResource(R.string.period_night)
}

@Composable
fun MarkerColorKey.localizedLabel(): String = when (this) {
    MarkerColorKey.HOTEL -> stringResource(R.string.item_type_hotel)
    MarkerColorKey.FLIGHT -> stringResource(R.string.item_type_flight)
    MarkerColorKey.RESTAURANT -> stringResource(R.string.item_type_restaurant)
    MarkerColorKey.ATTRACTION -> stringResource(R.string.item_type_attraction)
    MarkerColorKey.ACTIVITY -> stringResource(R.string.item_type_activity)
    MarkerColorKey.TRANSPORT -> stringResource(R.string.item_type_transport)
    MarkerColorKey.SHOW -> stringResource(R.string.item_type_show)
    MarkerColorKey.CUSTOM -> stringResource(R.string.item_type_custom)
}

@Composable
fun Role.localizedLabel(): String = when (this) {
    Role.OWNER -> stringResource(R.string.role_owner)
    Role.EDITOR -> stringResource(R.string.role_editor)
    Role.VIEWER -> stringResource(R.string.role_read_only)
}

@Composable
fun TripCountdown.localizedLabel(): String = when (this) {
    is TripCountdown.DaysAway -> pluralStringResource(R.plurals.trip_days_away, days.toInt(), days.toInt())
    TripCountdown.HappeningNow -> stringResource(R.string.trip_happening_now)
    TripCountdown.Completed -> stringResource(R.string.trip_completed)
}

/** Replaces WeatherCodeMapper.label() — moved here because it needs a Composable
 *  context to resolve stringResource(); WeatherCodeMapper.icon() is unaffected. */
@Composable
fun weatherConditionLabel(code: Int?): String = when (code) {
    0 -> stringResource(R.string.weather_clear_sky)
    1, 2, 3 -> stringResource(R.string.weather_partly_cloudy)
    45, 48 -> stringResource(R.string.weather_fog)
    51, 53, 55 -> stringResource(R.string.weather_drizzle)
    56, 57 -> stringResource(R.string.weather_freezing_drizzle)
    61, 63, 65 -> stringResource(R.string.weather_rain)
    66, 67 -> stringResource(R.string.weather_freezing_rain)
    71, 73, 75 -> stringResource(R.string.weather_snow)
    77 -> stringResource(R.string.weather_snow_grains)
    80, 81, 82 -> stringResource(R.string.weather_rain_showers)
    85, 86 -> stringResource(R.string.weather_snow_showers)
    95 -> stringResource(R.string.weather_thunderstorm)
    96, 99 -> stringResource(R.string.weather_thunderstorm_hail)
    else -> stringResource(R.string.weather_generic)
}