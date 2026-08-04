package com.tripro.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Raw result of comparing a trip's date range against today. Kept as plain data (not a
 *  formatted String) so the actual display text can be localized where a Composable is
 *  available — see [localizedLabel] in DisplayLabels.kt for the plurals-aware
 *  "N days away" formatting. */
sealed class TripCountdown {
    data class DaysAway(val days: Long) : TripCountdown()
    data object HappeningNow : TripCountdown()
    data object Completed : TripCountdown()
}

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Android keeps Locale.getDefault() in sync with the app's current effective
     *  locale automatically whenever a new Configuration is delivered to the process
     *  (including per-app language changes via LocaleManager) — no Context needed. */
    private fun displayLocale(): Locale = Locale.getDefault()

    fun parse(date: String): LocalDate = LocalDate.parse(date, isoFormatter)

    /** "Oct 12 - Oct 18, 2023" */
    fun formatRange(start: String, end: String): String {
        val s = parse(start)
        val e = parse(end)
        val locale = displayLocale()
        val monthDay = DateTimeFormatter.ofPattern("MMM d", locale)
        return if (s.year == e.year) {
            "${s.format(monthDay)} - ${e.format(monthDay)}, ${e.year}"
        } else {
            "${s.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))} - ${e.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))}"
        }
    }

    /** "Tuesday, Oct 14" */
    fun formatFullDayLabel(date: String): String {
        val d = parse(date)
        val locale = displayLocale()
        return "${d.dayOfWeek.getDisplayName(TextStyle.FULL, locale)}, ${d.format(DateTimeFormatter.ofPattern("MMM d", locale))}"
    }

    /** "Thu" */
    fun formatWeekdayShort(date: String): String {
        val locale = displayLocale()
        return parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)
    }

    /** "12" */
    fun formatDayNumber(date: String): String = parse(date).dayOfMonth.toString()

    /** Raw comparison result for the trips-list countdown badge — turn into display text
     *  via [localizedLabel] (DisplayLabels.kt), which needs a Composable context. */
    fun countdown(startDate: String, endDate: String): TripCountdown {
        val today = LocalDate.now()
        val start = parse(startDate)
        val end = parse(endDate)
        return when {
            today.isBefore(start) -> TripCountdown.DaysAway(java.time.temporal.ChronoUnit.DAYS.between(today, start))
            !today.isAfter(end) -> TripCountdown.HappeningNow
            else -> TripCountdown.Completed
        }
    }
}