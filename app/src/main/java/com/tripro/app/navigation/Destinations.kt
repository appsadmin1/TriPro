package com.tripro.app.navigation

object Destinations {
    const val LOGIN = "login"
    const val TRIPS_LIST = "trips"
    const val CREATE_TRIP = "trips/create"
    const val TRIP_OVERVIEW = "trip/{tripId}"
    const val DAY_DETAIL = "trip/{tripId}/day/{date}"
    const val COLLABORATORS = "trip/{tripId}/collaborators"
    const val TRIP_DOCS = "trip/{tripId}/docs"
    const val SETTINGS = "settings"

    fun tripOverview(tripId: String) = "trip/$tripId"
    fun dayDetail(tripId: String, date: String) = "trip/$tripId/day/$date"
    fun collaborators(tripId: String) = "trip/$tripId/collaborators"
    fun tripDocs(tripId: String) = "trip/$tripId/docs"

    const val ARG_TRIP_ID = "tripId"
    const val ARG_DATE = "date"
}