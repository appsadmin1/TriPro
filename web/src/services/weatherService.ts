import { DailyWeather, WeatherStatus } from "../data/models";
import { format, differenceInDays, parseISO, subDays } from "date-fns";

const FORECAST_HORIZON_DAYS = 16;
const PAST_HORIZON_DAYS = 92;

export const weatherService = {
  getDailyWeather: async (
    lat: number | undefined,
    lng: number | undefined,
    date: string
  ): Promise<DailyWeather> => {
    if (lat === undefined || lng === undefined) {
      return { date, status: WeatherStatus.NO_LOCATION };
    }

    const targetDate = parseISO(date);
    if (isNaN(targetDate.getTime())) {
      return { date, status: WeatherStatus.ERROR };
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const daysFromToday = differenceInDays(targetDate, today);

    if (daysFromToday > FORECAST_HORIZON_DAYS || daysFromToday < -PAST_HORIZON_DAYS) {
      return { date, status: WeatherStatus.NOT_YET_AVAILABLE };
    }

    try {
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&start_date=${date}&end_date=${date}`;

      const response = await fetch(url);
      if (!response.ok) {
        return { date, status: WeatherStatus.ERROR };
      }

      const data = await response.json();
      if (data.error) {
        return { date, status: WeatherStatus.ERROR };
      }

      const daily = data.daily;
      if (!daily || !daily.time || daily.time.length === 0) {
        return { date, status: WeatherStatus.ERROR };
      }

      return {
        date,
        status: WeatherStatus.AVAILABLE,
        weatherCode: daily.weather_code[0],
        tempMaxC: daily.temperature_2m_max[0],
        tempMinC: daily.temperature_2m_min[0],
        precipitationProbabilityPct: daily.precipitation_probability_max[0],
      };
    } catch (error) {
      console.error("Weather lookup failed", error);
      return { date, status: WeatherStatus.ERROR };
    }
  },

  forecastAvailableFrom: (date: string): string => {
    const targetDate = parseISO(date);
    const unlockDate = subDays(targetDate, FORECAST_HORIZON_DAYS);
    return format(unlockDate, "MMM d");
  },
};
