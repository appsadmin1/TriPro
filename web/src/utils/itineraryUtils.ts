import { ItineraryItem, DayPeriod, TimeType } from "../data/models";

export function getEffectivePeriod(item: ItineraryItem): DayPeriod {
  if (item.timeType === TimeType.PERIOD && item.period) {
    return item.period;
  }

  const time = item.startTime;
  if (!time) return DayPeriod.MORNING; // Fallback

  const parts = time.split(":");
  const hour = parseInt(parts[0], 10) || 0;

  if (hour >= 5 && hour <= 10) return DayPeriod.MORNING;
  if (hour >= 11 && hour <= 15) return DayPeriod.NOON;
  if (hour >= 16 && hour <= 18) return DayPeriod.AFTERNOON;
  if (hour >= 19 && hour <= 21) return DayPeriod.EVENING;
  return DayPeriod.NIGHT;
}

export function getTimeHeaderLabel(item: ItineraryItem): string | null {
  if (item.timeType === TimeType.EXACT) return item.startTime || null;
  if (item.timeType === TimeType.RANGE) {
    if (item.startTime && item.endTime) {
      return `${item.startTime} - ${item.endTime}`;
    }
    return item.startTime || null;
  }
  return null;
}

export interface TimeGroup {
  label: string | null;
  items: ItineraryItem[];
}

export interface PeriodGroup {
  period: DayPeriod;
  timeGroups: TimeGroup[];
}

export function groupByHierarchy(items: ItineraryItem[]): PeriodGroup[] {
  const periods = [
    DayPeriod.MORNING,
    DayPeriod.NOON,
    DayPeriod.AFTERNOON,
    DayPeriod.EVENING,
    DayPeriod.NIGHT,
  ];

  return periods
    .map((p) => {
      const itemsInPeriod = items.filter((it) => getEffectivePeriod(it) === p);
      if (itemsInPeriod.length === 0) return null;

      const timeGroups: TimeGroup[] = [];
      let currentLabel: string | null = null;
      let currentItems: ItineraryItem[] = [];

      itemsInPeriod.forEach((item) => {
        const label = getTimeHeaderLabel(item);
        if (label === currentLabel && label !== null) {
          currentItems.push(item);
        } else {
          if (currentItems.length > 0) {
            timeGroups.push({ label: currentLabel, items: currentItems });
          }
          currentLabel = label;
          currentItems = [item];
        }
      });

      if (currentItems.length > 0) {
        timeGroups.push({ label: currentLabel, items: currentItems });
      }

      return { period: p, timeGroups };
    })
    .filter((g): g is PeriodGroup => g !== null);
}
