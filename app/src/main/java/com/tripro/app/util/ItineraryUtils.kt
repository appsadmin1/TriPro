package com.tripro.app.util

import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.TimeType

object ItineraryUtils {

    fun getEffectivePeriod(item: ItineraryItem): DayPeriod {
        if (item.timeType == TimeType.PERIOD && item.period != null) {
            return item.period
        }
        
        val time = item.startTime ?: return DayPeriod.MORNING // Fallback
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        
        return when (hour) {
            in 5..10 -> DayPeriod.MORNING
            in 11..15 -> DayPeriod.NOON
            in 16..18 -> DayPeriod.AFTERNOON
            in 19..21 -> DayPeriod.EVENING
            else -> DayPeriod.NIGHT
        }
    }

    fun getTimeHeaderLabel(item: ItineraryItem): String? {
        return when (item.timeType) {
            TimeType.EXACT -> item.startTime
            TimeType.RANGE -> {
                if (item.startTime != null && item.endTime != null) {
                    "${item.startTime} - ${item.endTime}"
                } else {
                    item.startTime
                }
            }
            TimeType.PERIOD -> null
        }
    }
}

data class TimeGroup(
    val label: String?,
    val items: List<ItineraryItem>
)

data class PeriodGroup(
    val period: DayPeriod,
    val timeGroups: List<TimeGroup>
)

fun List<ItineraryItem>.groupByHierarchy(): List<PeriodGroup> {
    return DayPeriod.entries.mapNotNull { p ->
        val itemsInPeriod = this.filter { ItineraryUtils.getEffectivePeriod(it) == p }
        if (itemsInPeriod.isEmpty()) return@mapNotNull null
        
        // Secondary grouping by time label
        val timeGroups = mutableListOf<TimeGroup>()
        var currentLabel: String? = null
        var currentItems = mutableListOf<ItineraryItem>()
        
        itemsInPeriod.forEach { item ->
            val label = ItineraryUtils.getTimeHeaderLabel(item)
            if (label == currentLabel && label != null) {
                currentItems.add(item)
            } else {
                if (currentItems.isNotEmpty()) {
                    timeGroups.add(TimeGroup(currentLabel, currentItems))
                }
                currentLabel = label
                currentItems = mutableListOf(item)
            }
        }
        if (currentItems.isNotEmpty()) {
            timeGroups.add(TimeGroup(currentLabel, currentItems))
        }
        
        PeriodGroup(p, timeGroups)
    }
}
