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
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

private const val PREFS_NAME = "tripro_prefs"
private const val KEY_LANGUAGE = "selected_language"

/**
 * Our own SharedPreferences-backed store is the single source of truth for the chosen
 * language. Earlier attempts routed this through AppCompatDelegate.setApplicationLocales()
 * — tested and confirmed NOT reliable on this device: getApplicationLocales() read back
 * empty immediately after being set, and it's undocumented/opaque why. Rather than
 * depend on an abstraction we can't verify, we track the choice ourselves.
 */
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

fun currentAppLanguage(context: Context): AppLanguage = LanguagePreference.get(context)

/** Switches the app's language. Persists to our own SharedPreferences (the real source
 *  of truth) and best-effort informs the platform LocaleManager on API 33+ (mainly so
 *  the system Settings > App languages screen stays consistent — not depended on for
 *  our own UI to update). The caller must still call recreateActivity() afterward. */
fun setAppLanguage(context: Context, language: AppLanguage) {
    LanguagePreference.set(context, language)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(language.code)
        }
    }
}

/** Wraps [this] with a Configuration locked to [language] — the mechanism we actually
 *  depend on for resource resolution (values-he/) and layout direction. Called from
 *  attachBaseContext() in both TriProApplication and MainActivity. */
fun Context.applyAppLocale(language: AppLanguage = LanguagePreference.get(this)): Context {
    val locale = Locale(language.code)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}

/**
 * Fully restarts MainActivity so a locale change takes effect. Activity.recreate() was
 * tried first — logcat confirmed it gets scheduled ("Schedule relaunch activity:
 * com.tripro.app.MainActivity") but then fails at the WindowManager level
 * ("onActivityLocalRelaunched: failed"), after which the Activity is just resumed rather
 * than actually re-created — no fresh onCreate() runs, so the new locale never takes
 * effect. finish() + a fresh startActivity() is a plain, ordinary Activity restart
 * instead of that special in-place relaunch path, sidestepping whatever is failing there.
 */
fun recreateActivity(context: Context) {
    val activity = context as? Activity ?: return
    val intent = activity.intent
    activity.startActivity(intent)
    activity.finish()
}