package com.tripro.app.data.model

import com.google.firebase.firestore.DocumentId

enum class ItemType(val icon: String) {
    FLIGHT("flight_takeoff"),
    HOTEL("hotel"),
    RESTAURANT("restaurant"),
    ATTRACTION("museum"),
    ACTIVITY("hiking"),
    TRANSPORT("directions_car"),
    CUSTOM("event")
}

/** How the user chose to specify timing for this item, per the "add specific hour(s) or
 *  proposed range or day time like morning/noon" requirement. */
enum class TimeType { EXACT, RANGE, PERIOD }

enum class DayPeriod(val label: String) {
    MORNING("Morning"),
    NOON("Noon"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night")
}

/**
 * Firestore document: trips/{tripId}/days/{date}/items/{itemId}
 */
data class ItineraryItem(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val type: ItemType = ItemType.CUSTOM,
    val timeType: TimeType = TimeType.PERIOD,
    val startTime: String? = null, // "HH:mm", used when timeType == EXACT or RANGE
    val endTime: String? = null,   // "HH:mm", used when timeType == RANGE
    val period: DayPeriod? = null, // used when timeType == PERIOD
    val locationName: String = "",
    val address: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    /** Special note/alert for this specific place, e.g. "18+ only" or
     *  "Closes early at 18:00 today" — rendered as the amber/red alert pill in the UI. */
    val note: String = "",
    val attachments: List<Attachment> = emptyList(),
    val order: Int = 0,
    val createdBy: String = "",
    val updatedBy: String = "" // uid of the last editor — used to exclude them from the
                                // "itinerary changed" push notification
) {
    /** Sort key so items render in chronological order regardless of how time was entered. */
    fun sortMinutes(): Int = when (timeType) {
        TimeType.EXACT, TimeType.RANGE -> startTime?.let { toMinutes(it) } ?: (order + 10_000)
        TimeType.PERIOD -> when (period) {
            DayPeriod.MORNING -> 6 * 60
            DayPeriod.NOON -> 12 * 60
            DayPeriod.AFTERNOON -> 14 * 60
            DayPeriod.EVENING -> 18 * 60
            DayPeriod.NIGHT -> 21 * 60
            null -> order + 10_000
        }
    }

    private fun toMinutes(hhmm: String): Int {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}
