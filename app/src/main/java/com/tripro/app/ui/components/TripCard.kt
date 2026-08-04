package com.tripro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.tripro.app.data.model.Trip
import com.tripro.app.util.DateUtils
import com.tripro.app.util.localizedLabel

@Composable
fun TripCard(
    trip: Trip,
    collaboratorPhotoUrls: List<String>,
    onClick: () -> Unit,
    isPast: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // DESIGN.md: Level 2 (Hover/Active) shadow: 0px 4px 20px rgba(0, 43, 91, 0.08)
    val ambientShadow = Color(0xFF002B5B).copy(alpha = 0.08f)
    
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, com.tripro.app.ui.theme.HorizonEthosColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) 8.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = ambientShadow,
                spotColor = ambientShadow
            )
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Accent Bar
            if (!isPast) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

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
                                .border(1.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(percent = 50))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                DateUtils.countdown(trip.startDate, trip.endDate).localizedLabel().uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(20.dp).padding(start = if (isPast) 0.dp else 8.dp)) {
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
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
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
}
