package com.tripro.app.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.forcedEnglish(): Context {
    val locale = Locale.ENGLISH
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}