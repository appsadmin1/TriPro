package com.tripro.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tripro.app.R

/**
 * A clean digital time entry dialog using Material3's `TimeInput`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTimePickerDialog(
    title: String = stringResource(R.string.time_picker_default_title),
    initial: String, // "HH:mm", 24-hour
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initial.split(":")
    val state = rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
        initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { 
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimeInput(state = state) 
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
