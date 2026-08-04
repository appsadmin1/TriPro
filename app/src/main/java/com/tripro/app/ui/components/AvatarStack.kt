package com.tripro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * DESIGN.md "Presence Indicators": 32px circular avatars, 2px Cloud White border,
 * overlapped in a stack. [extraCount] renders a "+N" tail bubble when there are more
 * collaborators than [maxVisible].
 */
@Composable
fun AvatarStack(
    photoUrls: List<String>,
    maxVisible: Int = 3,
    avatarSize: Int = 32
) {
    val visible = photoUrls.take(maxVisible)
    val extraCount = (photoUrls.size - maxVisible).coerceAtLeast(0)

    Row {
        visible.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarSize.dp)
                    .offset(x = (-12 * index).dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .size(avatarSize.dp)
                    .offset(x = (-12 * visible.size).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+$extraCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
