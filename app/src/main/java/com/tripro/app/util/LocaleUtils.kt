package com.tripro.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Languages TriPro supports choosing from the navigation drawer (see AppDrawerContent). */
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HEBREW("he", "עברית");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

/**
 * Reads/writes the person's chosen in-app language via AppCompatDelegate's per-app
 * language support (androidx.appcompat 1.6+) — Google's supported mechanism for "switch
 * this app's language independent of the device's system language."
 *
 * This replaced an earlier manual attachBaseContext()/createConfigurationContext()
 * approach, which correctly flipped Compose's layout direction (Compose derives
 * LocalLayoutDirection from the real Android Configuration, so once *any* mechanism
 * applies an RTL locale, mirroring follows) but did not reliably make stringResource()
 * resolve values-he/ strings — a known-fragile pattern with manually wrapped Contexts on
 * some OEM builds. AppCompatDelegate routes through Android 13+'s own LocaleManager where
 * available, with a well-tested polyfill below that, and reliably makes both follow.
 *
 * AppCompatDelegate persists the choice itself — no separate storage needed here.
 */
fun currentAppLanguage(): AppLanguage {
    val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    return AppLanguage.entries.firstOrNull { tag.startsWith(it.code) } ?: AppLanguage.ENGLISH
}

/** Switches the app's language. AppCompatDelegate recreates whichever activities need it
 *  to pick up the new locale — no manual Activity.recreate() call needed. */
fun setAppLanguage(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
}