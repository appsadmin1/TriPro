package com.tripro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tripro.app.data.model.DailyWeather
import com.tripro.app.data.model.WeatherStatus
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.util.WeatherCodeMapper
import kotlin.math.roundToInt

@Composable
fun WeatherCard(
    weather: DailyWeather?,
    isLoading: Boolean,
    forecastAvailableFromLabel: String?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp).size(28.dp), strokeWidth = 2.dp)
                    Text("Checking the forecast…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                weather == null || weather.status == WeatherStatus.NO_LOCATION -> {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(end = 16.dp))
                    Text(
                        "Add a hotel location to this day to see the forecast here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                weather.status == WeatherStatus.NOT_YET_AVAILABLE -> {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(end = 16.dp))
                    Column {
                        Text("Forecast not out yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (forecastAvailableFromLabel != null) "Check back from $forecastAvailableFromLabel — forecasts open up 16 days ahead."
                            else "Forecasts open up 16 days ahead of the date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                weather.status == WeatherStatus.ERROR -> {
                    Icon(Icons.Filled.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(end = 16.dp))
                    Text("Couldn't load the forecast right now.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> {
                    Icon(
                        iconFor(weather.weatherCode),
                        contentDescription = null,
                        tint = HorizonEthosColors.SecondaryFixedDim,
                        modifier = Modifier.size(36.dp).padding(end = 16.dp)
                    )
                    Column {
                        Text(
                            WeatherCodeMapper.label(weather.weatherCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val high = weather.tempMaxC?.roundToInt()
                        val low = weather.tempMinC?.roundToInt()
                        val rain = weather.precipitationProbabilityPct
                        Text(
                            buildString {
                                if (high != null && low != null) append("$low° / $high°C")
                                if (rain != null) append("  ·  $rain% chance of rain")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(code: Int?): ImageVector = when (code) {
    0 -> Icons.Filled.WbSunny
    1, 2, 3 -> Icons.Filled.WbCloudy
    45, 48 -> Icons.Filled.Cloud
    51, 53, 55, 56, 57 -> Icons.Filled.Grain
    61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Filled.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Filled.AcUnit
    95, 96, 99 -> Icons.Filled.Thunderstorm
    else -> Icons.Filled.WbCloudy
}
