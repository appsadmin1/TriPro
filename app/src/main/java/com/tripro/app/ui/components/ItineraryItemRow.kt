package com.tripro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.TimeType
import com.tripro.app.ui.theme.HorizonEthosColors

@Composable
fun ItineraryItemRow(
    item: ItineraryItem,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddAttachment: () -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        // Time column
        Column(modifier = Modifier.width(64.dp).padding(top = 4.dp)) {
            when (item.timeType) {
                TimeType.EXACT -> Text(
                    item.startTime ?: "--:--",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TimeType.RANGE -> {
                    Text(item.startTime ?: "--:--", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(item.endTime ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TimeType.PERIOD -> {
                    val label = (item.period ?: DayPeriod.MORNING).label
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        label.uppercase().forEach { char ->
                            Text(
                                char.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVectorForType(item.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (item.type == ItemType.CUSTOM && item.customTypeName.isNotBlank()) item.customTypeName else item.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (item.locationName.isNotBlank()) {
                                Text(
                                    item.locationName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.note.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        item.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            if (item.attachments.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    items(item.attachments) { attachment ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(percent = 50))
                                                .border(1.dp, HorizonEthosColors.CardBorder, RoundedCornerShape(percent = 50))
                                                .clickable { onAttachmentClick(attachment) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.AttachFile,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            Text(attachment.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (canEdit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, HorizonEthosColors.CardBorder))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Add Note", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = onAddAttachment) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("Upload File", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun imageVectorForType(type: ItemType): ImageVector = when (type) {
    ItemType.FLIGHT -> Icons.Filled.FlightTakeoff
    ItemType.HOTEL -> Icons.Filled.Hotel
    ItemType.RESTAURANT -> Icons.Filled.Restaurant
    ItemType.ATTRACTION -> Icons.Filled.Museum
    ItemType.ACTIVITY -> Icons.Filled.Hiking
    ItemType.TRANSPORT -> Icons.Filled.DirectionsCar
    ItemType.SHOW -> Icons.Filled.TheaterComedy
    ItemType.CUSTOM -> Icons.Filled.Event
}
