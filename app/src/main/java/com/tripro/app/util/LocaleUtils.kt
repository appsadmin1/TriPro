package com.tripro.app.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

/** Languages TriPro supports choosing from the navigation drawer (see AppDrawerContent). */
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HEBREW("he", "עברית");

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

private const val PREFS_NAME = "tripro_prefs"
private const val KEY_LANGUAGE = "selected_language"

/** Persists the user's chosen in-app language across process restarts — a plain
 *  SharedPreferences read/write (not tied to AppContainer) so it's readable from
 *  attachBaseContext(), which runs before AppContainer exists. */
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
 * Wraps [this] with a Configuration locked to [language] (defaulting to whatever's
 * persisted). TriPro's language is an explicit in-app choice rather than a follower of
 * the device's system locale — this used to unconditionally force English regardless of
 * device locale; it's now generalized so Hebrew can be chosen the same way from
 * AppDrawerContent's language picker.
 *
 * Both the Locale *and* LayoutDirection are set here; ui/theme/Theme.kt separately
 * forces Compose's LocalLayoutDirection to match — the two need to agree, or content
 * Android renders outside Compose would mismatch Compose's own layout direction.
 */
fun Context.applyAppLocale(language: AppLanguage = LanguagePreference.get(this)): Context {
    val locale = Locale(language.code)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}

/** Restarts the current Activity so a locale change (Locale + LayoutDirection, both set
 *  at attachBaseContext time) actually takes effect. */
fun Context.recreateActivity() {
    (this as? Activity)?.recreate()
}