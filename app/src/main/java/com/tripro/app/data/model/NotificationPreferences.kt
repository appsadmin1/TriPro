package com.tripro.app.data.model

data class NotificationPreferences(
    val tripInvites: Boolean = true,
    val itineraryChanges: Boolean = true,
    val dayInfoChanges: Boolean = true
)