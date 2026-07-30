package com.tripro.app.navigation

object Destinations {
    const val LOGIN = "login"

    // "trips/list/{filter}" (3 segments) deliberately doesn't collide with the
    // 2-segment "trips/create" — Navigation-Compose matches route *patterns*, and two
    // patterns that could both match the same literal path is a real footgun.
    const val TRIPS_LIST = "trips/list/{filter}"
    const val CREATE_TRIP = "trips/create"
    const val TRIP_OVERVIEW = "trip/{tripId}"
    const val DAY_DETAIL = "trip/{tripId}/day/{date}"
    const val COLLABORATORS = "trip/{tripId}/collaborators"
    const val TRIP_DOCUMENTS = "trip/{tripId}/documents"
    const val ALERTS = "alerts"
    const val PROFILE = "profile"

    fun tripsList(filter: String = "all") = "trips/list/$filter"
    fun tripOverview(tripId: String) = "trip/$tripId"
    fun dayDetail(tripId: String, date: String) = "trip/$tripId/day/$date"
    fun collaborators(tripId: String) = "trip/$tripId/collaborators"
    fun tripDocuments(tripId: String) = "trip/$tripId/documents"

    const val ARG_TRIP_ID = "tripId"
    const val ARG_DATE = "date"
    const val ARG_FILTER = "filter"
}