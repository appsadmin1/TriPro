package com.tripro.app.navigation

/**
 * Carries the destination encoded in a push notification's tap action.
 * MainActivity builds this from the launching Intent's extras (see NotificationHelper for
 * where those extras are set); TriProNavGraph consumes it once via LaunchedEffect and then
 * it's discarded so rotating the screen doesn't re-trigger the navigation.
 */
data class PendingDeepLink(
    val tripId: String,
    val date: String? = null
)
