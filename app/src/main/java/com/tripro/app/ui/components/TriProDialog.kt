package com.tripro.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tripro.app.ui.theme.PillShape
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProShapes
import com.tripro.app.ui.theme.TriProTypography

@Composable
fun TriProAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    dismissButtonText: String? = null,
    onDismiss: (() -> Unit)? = null,
    title: String,
    text: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = TriProColors.Primary,
    isDestructive: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = TriProShapes.extraLarge,
        containerColor = TriProColors.SurfaceContainerLowest,
        tonalElevation = 0.dp,
        modifier = Modifier.border(
            width = 1.dp,
            color = TriProColors.OutlineVariant.copy(alpha = 0.3f),
            shape = TriProShapes.extraLarge
        ),
        icon = icon?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isDestructive) TriProColors.Error else iconColor
                )
            }
        },
        title = {
            Text(
                text = title,
                style = TriProTypography.headlineLarge.copy(
                    color = TriProColors.Primary
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = content ?: text?.let {
            {
                Text(
                    text = it,
                    style = TriProTypography.bodyMedium,
                    color = TriProColors.OnSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) TriProColors.Error else TriProColors.Primary,
                    contentColor = TriProColors.OnPrimary
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    confirmButtonText,
                    style = TriProTypography.labelMedium
                )
            }
        },
        dismissButton = dismissButtonText?.let {
            {
                TextButton(
                    onClick = { onDismiss?.invoke() ?: onDismissRequest() },
                    shape = PillShape,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        it,
                        color = TriProColors.OnSurfaceVariant,
                        style = TriProTypography.labelMedium
                    )
                }
            }
        }
    )
}

@Composable
fun TriProDialog(
    onDismissRequest: () -> Unit,
    showCloseButton: Boolean = true,
    padding: PaddingValues = PaddingValues(32.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        androidx.compose.material3.Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 40.dp, bottom = 80.dp)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = TriProColors.OutlineVariant.copy(alpha = 0.3f),
                    shape = TriProShapes.extraLarge
                ),
            shape = TriProShapes.extraLarge,
            color = TriProColors.SurfaceContainerLowest,
            tonalElevation = 0.dp
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(padding),
                    content = content
                )

                if (showCloseButton) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TriProColors.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
