package com.tripro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripro.app.data.model.Trip
import com.tripro.app.util.DateUtils

@Composable
fun TripCard(
    trip: Trip,
    collaboratorPhotoUrls: List<String>,
    onClick: () -> Unit,
    isPast: Boolean = false
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, com.tripro.app.ui.theme.HorizonEthosColors.CardBorder),
        // A little resting lift (was 0dp) plus a stronger press response, so the list
        // reads as a stack of cards rather than flat rectangles on the background.
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 1.dp else 3.dp, pressedElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
                Box {
                    AsyncImage(
                        model = trip.coverImageUrl,
                        contentDescription = trip.destination,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isPast) 160.dp else 192.dp)
                    )
                    if (!isPast) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.9f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                DateUtils.countdownLabel(trip.startDate, trip.endDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        trip.name,
                        style = if (isPast) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, bottom = if (isPast) 0.dp else 16.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            DateUtils.formatRange(trip.startDate, trip.endDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isPast) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarStack(photoUrls = collaboratorPhotoUrls)
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
