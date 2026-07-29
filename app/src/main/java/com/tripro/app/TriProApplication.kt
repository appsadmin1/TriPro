package com.tripro.app

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.tripro.app.notifications.NotificationHelper

class TriProApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        // Powers the "search on Google Maps" pickers for hotels, airports, and
        // itinerary stops (see util/PlacesAutocomplete.kt). Reuses the same Google
        // Cloud API key as the Maps SDK (MAPS_API_KEY) — just make sure the **Places
        // API** is also enabled for that key in Google Cloud Console; Maps SDK and
        // Places API are billed/enabled separately even though they share one key.
        // See README "Setup" step 5.
        if (!Places.isInitialized() && BuildConfig.MAPS_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}
