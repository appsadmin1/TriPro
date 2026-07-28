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

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}
