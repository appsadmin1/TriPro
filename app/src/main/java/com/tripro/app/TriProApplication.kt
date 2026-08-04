package com.tripro.app

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import com.google.android.libraries.places.api.Places
import com.tripro.app.notifications.NotificationHelper
import com.tripro.app.util.AppLanguage
import com.tripro.app.util.applyAppLocale

class TriProApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        // No-op on API 33+ — see applyAppLocale()'s doc comment.
        super.attachBaseContext(base.applyAppLocale())
    }

    override fun onCreate() {
        super.onCreate()

        // TriPro ships English-only content by design unless the person
        // explicitly picks Hebrew from the drawer — so on first run (before
        // LocaleManager has any per-app language recorded), force English rather than
        // following the device's system locale. LocaleManager persists the choice
        // itself once set, so this only fires once per install on API 33+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            if (localeManager?.applicationLocales?.isEmpty == true) {
                localeManager.applicationLocales = LocaleList.forLanguageTags(AppLanguage.ENGLISH.code)
            }
        }

        container = AppContainer(this)
        NotificationHelper.createChannel(this)

        if (!Places.isInitialized() && BuildConfig.MAPS_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}