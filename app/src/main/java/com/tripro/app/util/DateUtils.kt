package com.tripro.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(date: String): LocalDate = LocalDate.parse(date, isoFormatter)

    /** "Oct 12 - Oct 18, 2023" */
    fun formatRange(start: String, end: String): String {
        val s = parse(start)
        val e = parse(end)
        val monthDay = DateTimeFormatter.ofPattern("MMM d")
        return if (s.year == e.year) {
            "${s.format(monthDay)} - ${e.format(monthDay)}, ${e.year}"
        } else {
            "${s.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))} - ${e.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
        }
    }

    /** "Tuesday, Oct 14" */
    fun formatFullDayLabel(date: String): String {
        val d = parse(date)
        return "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${d.format(DateTimeFormatter.ofPattern("MMM d"))}"
    }

    /** "Thu" */
    fun formatWeekdayShort(date: String): String =
        parse(date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase()

    /** "12" */
    fun formatDayNumber(date: String): String = parse(date).dayOfMonth.toString()

    /** "4 days away" / "Today" / "In progress" style countdown for the trips list. */
    fun countdownLabel(startDate: String, endDate: String): String {
        val today = LocalDate.now()
        val start = parse(startDate)
        val end = parse(endDate)
        return when {
            today.isBefore(start) -> {
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, start)
                if (days == 1L) "1 DAY AWAY" else "$days DAYS AWAY"
            }
            !today.isAfter(end) -> "HAPPENING NOW"
            else -> "COMPLETED"
        }
    }
}
