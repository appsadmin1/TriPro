package com.tripro.app.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Firestore document: trips/{tripId}/days/{date}
 * Document id IS the date string ("2023-10-14") so a day can be looked up directly
 * without a query, and creating trip.startDate..trip.endDate day docs is a cheap batch write.
 */
data class TripDay(
    @DocumentId
    val date: String = "", // yyyy-MM-dd
    val dayIndex: Int = 0, // 1-based day number within the trip, for "Day 3" style headers
    val hotel: HotelInfo? = null,
    val flight: FlightInfo? = null,
    val dayNote: String = "", // free-text note for the whole day (e.g. "Pack light layers")
    val updatedBy: String = "" // uid of the last person to edit hotel/flight/dayNote — used
                                // to exclude them from the "this day changed" push notification
)

data class HotelInfo(
    val name: String = "",
    val address: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val placeId: String? = null,
    val checkIn: String = "",
    val checkOut: String = "",
    val arrivalTime: String = "",
    val notes: String = ""
)

data class FlightInfo(
    val airline: String = "",
    val flightNumber: String = "",
    val departureAirportCode: String = "",
    val arrivalAirportCode: String = "",
    val departureAirportLat: Double? = null,
    val departureAirportLng: Double? = null,
    val arrivalAirportLat: Double? = null,
    val arrivalAirportLng: Double? = null,
    val departureTime: String = "", // "HH:mm", local to departure airport
    val arrivalTime: String = "",
    val notes: String = ""
)
