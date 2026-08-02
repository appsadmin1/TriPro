package com.tripro.app

import android.app.Application
import android.content.Context
import com.google.android.libraries.places.api.Places
import com.tripro.app.notifications.NotificationHelper
import com.tripro.app.util.applyAppLocale

class TriProApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.applyAppLocale())
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        if (!Places.isInitialized() && BuildConfig.MAPS_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}