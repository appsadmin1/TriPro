package com.tripro.app.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** Languages TriPro supports choosing from the navigation drawer (see AppDrawerContent). */
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HEBREW("he", "עברית");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code == null) return ENGLISH
            // Android uses "iw" internally for Hebrew; match both "he" and "iw"
            if (code == "he" || code == "iw") return HEBREW
            return entries.firstOrNull { it.code == code } ?: ENGLISH
        }
    }
}

private const val PREFS_NAME = "tripro_prefs"
private const val KEY_LANGUAGE = "selected_language"

/** Fallback store for API < 33, where there's no platform LocaleManager to read from. */
object LanguagePreference {
    fun get(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromCode(prefs.getString(KEY_LANGUAGE, null))
    }

    fun set(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LANGUAGE, language.code)
        }
    }
}

/**
 * Reads the currently active app language.
 *
 * On API 33+, LocaleManager is the real source of truth — confirmed via logcat
 * ("Checking to restart ... changed={CONFIG_LOCALE...}") to correctly drive Android's
 * own Activity recreation and resource resolution end-to-end once setAppLanguage() has
 * called LocaleManager.setApplicationLocales(). Below 33 there's no such API, so we read
 * our own SharedPreferences fallback instead.
 */
fun currentAppLanguage(context: Context): AppLanguage {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val tag = context.getSystemService(LocaleManager::class.java)?.applicationLocales?.toLanguageTags()
        if (!tag.isNullOrBlank()) {
            return AppLanguage.entries.firstOrNull { tag.startsWith(it.code) } ?: AppLanguage.ENGLISH
        }
    }
    return LanguagePreference.get(context)
}

/**
 * Switches the app's language.
 *
 * On API 33+, LocaleManager.setApplicationLocales() alone is sufficient — Android
 * detects the Configuration change and destroys+recreates MainActivity on its own with
 * the new locale already correctly applied (confirmed working via logcat). Nothing else
 * should touch the Configuration on this path — see applyAppLocale()/recreateActivity()
 * below, both now no-ops on 33+. Doing both was the actual cause of the crash: our own
 * manual recreate was racing the system's automatic one for the same locale change.
 *
 * Below API 33, there's no platform mechanism, so we persist our own preference and the
 * caller (recreateActivity()) must still trigger a manual Activity restart.
 */
fun setAppLanguage(context: Context, language: AppLanguage) {
    // Always persist to SharedPreferences as a fallback and for consistency
    LanguagePreference.set(context, language)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)?.applicationLocales =
            LocaleList.forLanguageTags(language.code)
    }
}

/** Wraps [this] with a Configuration locked to the saved language — only meaningful on
 *  API < 33. On 33+ this is a deliberate no-op: LocaleManager already delivers the
 *  correct Configuration to a freshly recreated Activity on its own, and re-wrapping it
 *  here was actively downgrading the system's correct [he, he_IL] LocaleList down to a
 *  bare [he] right as the Activity was being recreated — see the "Updating
 *  configuration, locales updated from [he,he_IL] to [he]" line in logcat, which was
 *  this function running, not the system. */
fun Context.applyAppLocale(language: AppLanguage = LanguagePreference.get(this)): Context {
    val locale = Locale(language.code)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // On API 33+, prefer the LocaleList from the system if available
        val systemLocales = getSystemService(LocaleManager::class.java)?.applicationLocales
        if (systemLocales != null && !systemLocales.isEmpty) {
            config.setLocales(systemLocales)
        } else {
            config.setLocales(LocaleList(locale))
        }
    } else {
        config.setLocale(locale)
    }

    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}

/** Restarts the current Activity — only meaningful on API < 33. On 33+ this is a
 *  deliberate no-op: LocaleManager's own Configuration change already triggers
 *  Android's normal Activity recreation automatically; calling this too is what raced
 *  with it and crashed the app. */
fun recreateActivity(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
    (context as? Activity)?.recreate()
}