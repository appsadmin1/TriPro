package com.tripro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripro.app.R
import com.tripro.app.data.model.Attachment
import com.tripro.app.data.model.DayPeriod
import com.tripro.app.data.model.ItemType
import com.tripro.app.data.model.ItineraryItem
import com.tripro.app.data.model.NoteType
import com.tripro.app.data.model.TimeType
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.util.localizedLabel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.material.icons.filled.PriorityHigh

import androidx.compose.ui.graphics.Color
import com.tripro.app.data.model.ActivityColorPrefs
import com.tripro.app.data.model.toMarkerColorKey

@Composable
fun ItineraryItemRow(
    item: ItineraryItem,
    activityColors: ActivityColorPrefs,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddAttachment: () -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSynthetic = item.id.startsWith("synthetic_")

    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(modifier = Modifier.width(100.dp).padding(top = 8.dp)) {
            when (item.timeType) {
                TimeType.EXACT -> Text(item.startTime ?: "--:--", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                TimeType.RANGE -> {
                    Text(item.startTime ?: "--:--", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    Text(item.endTime ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                TimeType.PERIOD -> Text(
                    (item.period ?: DayPeriod.MORNING).localizedLabel(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        val accentColor = Color(activityColors.colorInt(item.type.toMarkerColorKey()))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.dp, HorizonEthosColors.CardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.weight(1f).shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(accentColor)
                )

                Column {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.weight(1f)) {
                            val iconBg = accentColor.copy(alpha = 0.12f)
                            val iconTint = accentColor
                            
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(iconBg)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVectorForType(item.type),
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                if (item.type == ItemType.CUSTOM && item.customLabel.isNotBlank()) {
                                    Text(item.customLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                if (item.note.isNotBlank()) {
                                    val isAlert = item.noteType == NoteType.ALERT
                                    val bg = if (isAlert) MaterialTheme.colorScheme.errorContainer else HorizonEthosColors.Success.copy(alpha = 0.25f)
                                    val fg = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else HorizonEthosColors.Success
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            if (isAlert) Icons.Filled.Warning else Icons.Filled.PriorityHigh,
                                            contentDescription = null,
                                            tint = fg,
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            item.note,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = fg
                                        )
                                    }
                                }

                                if (item.locationName.isNotBlank() && item.note.isBlank()) {
                                    Text(item.locationName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                if (item.attachments.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        item.attachments.forEach { attachment ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(percent = 50))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                    .border(1.dp, HorizonEthosColors.CardBorder, RoundedCornerShape(percent = 50))
                                                    .clickable { onAttachmentClick(attachment) }
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                val attachmentIcon = if (attachment.fileName.endsWith(".pdf", ignoreCase = true)) Icons.Filled.PictureAsPdf else Icons.Filled.AttachFile
                                                val attachmentTint = if (attachment.fileName.endsWith(".pdf", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                
                                                Icon(attachmentIcon, contentDescription = null, tint = attachmentTint, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                                                Text(attachment.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (canEdit) {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.itinerary_row_edit_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (canEdit && !isSynthetic) {
                        Row(
                            modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, HorizonEthosColors.CardBorder.copy(alpha = 0.5f))).padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            TextButton(onClick = onEdit) {
                                Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                Text(stringResource(R.string.itinerary_row_add_note), style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = onAddAttachment) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                Text(stringResource(R.string.itinerary_row_upload_file), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.itinerary_row_delete_cd), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
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
