package com.tripro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProShapes
import com.tripro.app.ui.theme.TriProTypography

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment

@Composable
fun TriProTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TriProTypography.labelMedium,
            color = TriProColors.OnSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TriProTypography.bodyMedium.copy(color = TriProColors.OnSurface),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions,
            cursorBrush = SolidColor(TriProColors.Primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TriProShapes.medium)
                        .background(TriProColors.SurfaceContainerLow)
                        .border(
                            width = 1.dp,
                            color = TriProColors.OutlineVariant.copy(alpha = 0.3f),
                            shape = TriProShapes.medium
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            leadingIcon()
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = TriProTypography.bodyMedium,
                                color = TriProColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
