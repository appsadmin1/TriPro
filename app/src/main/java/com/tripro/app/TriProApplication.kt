package com.tripro.app

import android.app.Application
import com.tripro.app.notifications.NotificationHelper

class TriProApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
    }
}
