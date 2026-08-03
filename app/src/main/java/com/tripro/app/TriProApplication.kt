package com.tripro.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.libraries.places.api.Places
import com.tripro.app.notifications.NotificationHelper
import com.tripro.app.util.AppLanguage

class TriProApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Horizon Ethos ships English-only content by design unless the person explicitly
        // picks Hebrew from the drawer — so on first run (before AppCompatDelegate has any
        // per-app language recorded), force English rather than following the device's
        // system locale. Once a language is explicitly chosen, AppCompatDelegate remembers
        // it across restarts on its own, so this only fires once per install.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(AppLanguage.ENGLISH.code))
        }

        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        if (!Places.isInitialized() && BuildConfig.MAPS_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}