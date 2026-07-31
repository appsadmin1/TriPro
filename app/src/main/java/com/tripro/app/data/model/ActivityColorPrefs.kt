package com.tripro.app.data.model

import android.graphics.Color as AndroidColor

/** One marker/legend color per kind of pin on a day's map. HOTEL is a pseudo-type —
 *  hotels aren't itinerary items, but they get their own pin, so their own color too. */
enum class MarkerColorKey(val displayLabel: String) {
    HOTEL("Hotel"),
    FLIGHT("Flight"),
    RESTAURANT("Restaurant"),
    ATTRACTION("Attraction"),
    ACTIVITY("Activity"),
    TRANSPORT("Transport"),
    SHOW("Show"),
    CUSTOM("Custom")
}

fun ItemType.toMarkerColorKey(): MarkerColorKey = when (this) {
    ItemType.FLIGHT -> MarkerColorKey.FLIGHT
    ItemType.HOTEL -> MarkerColorKey.HOTEL
    ItemType.RESTAURANT -> MarkerColorKey.RESTAURANT
    ItemType.ATTRACTION -> MarkerColorKey.ATTRACTION
    ItemType.ACTIVITY -> MarkerColorKey.ACTIVITY
    ItemType.TRANSPORT -> MarkerColorKey.TRANSPORT
    ItemType.SHOW -> MarkerColorKey.SHOW
    ItemType.CUSTOM -> MarkerColorKey.CUSTOM
}

val DefaultActivityColorHex: Map<MarkerColorKey, String> = mapOf(
    MarkerColorKey.HOTEL to "#405F91",
    MarkerColorKey.FLIGHT to "#7594CA",
    MarkerColorKey.RESTAURANT to "#F9AD00",
    MarkerColorKey.ATTRACTION to "#8E44AD",
    MarkerColorKey.ACTIVITY to "#10B981",
    MarkerColorKey.TRANSPORT to "#43474F",
    MarkerColorKey.SHOW to "#BA1A1A",
    MarkerColorKey.CUSTOM to "#747780"
)

/** Curated palette offered in the Settings color picker. */
val MarkerColorPalette: List<String> = listOf(
    "#405F91", "#7594CA", "#F9AD00", "#8E44AD", "#10B981", "#43474F", "#BA1A1A",
    "#747780", "#E67E22", "#16A085", "#D35400", "#2C3E50"
)

data class ActivityColorPrefs(val hexByKey: Map<MarkerColorKey, String> = DefaultActivityColorHex) {
    fun colorInt(key: MarkerColorKey): Int {
        val hex = hexByKey[key] ?: DefaultActivityColorHex.getValue(key)
        return runCatching { AndroidColor.parseColor(hex) }
            .getOrElse { AndroidColor.parseColor(DefaultActivityColorHex.getValue(key)) }
    }
    fun hex(key: MarkerColorKey): String = hexByKey[key] ?: DefaultActivityColorHex.getValue(key)
}