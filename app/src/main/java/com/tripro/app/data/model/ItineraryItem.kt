package com.tripro.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

enum class ItemType(val icon: String) {
    FLIGHT("flight_takeoff"),
    HOTEL("hotel"),
    RESTAURANT("restaurant"),
    ATTRACTION("museum"),
    ACTIVITY("hiking"),
    TRANSPORT("directions_car"),
    SHOW("theater_comedy"),
    CUSTOM("event")
}

enum class TimeType { EXACT, RANGE, PERIOD }

enum class DayPeriod(val label: String) {
    MORNING("Morning"), NOON("Noon"), AFTERNOON("Afternoon"), EVENING("Evening"), NIGHT("Night")
}

/** Item 3: whether [ItineraryItem.note] renders as a red "Alert" (warning icon) or a
 *  green "Note" (exclamation icon). Defaults to ALERT so every note written before this
 *  field existed keeps its old red/warning look. */
enum class NoteType { ALERT, NOTE }

@IgnoreExtraProperties
data class ItineraryItem(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val type: ItemType = ItemType.CUSTOM,
    val timeType: TimeType = TimeType.PERIOD,
    val startTime: String? = null,
    val endTime: String? = null,
    val period: DayPeriod? = null,
    val locationName: String = "",
    val address: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val note: String = "",
    val noteType: NoteType = NoteType.ALERT,
    val customLabel: String = "",
    val flightInfo: FlightInfo? = null,
    val hotelInfo: HotelInfo? = null,
    val attachments: List<Attachment> = emptyList(),
    val order: Int = 0,
    /** Denormalized parent trip id — lets the "View Docs" screen run one
     *  collectionGroup("items") query scoped to a single trip instead of reading every
     *  day's items subcollection one at a time. Only set going forward (see
     *  TripRepository.addItem/updateItem); items created before this field existed read
     *  back as "" and are simply skipped by that screen. */
    val tripId: String = "",
    val createdBy: String = "",
    val updatedBy: String = ""
) {
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