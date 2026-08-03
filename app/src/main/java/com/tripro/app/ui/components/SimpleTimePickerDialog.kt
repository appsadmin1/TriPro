package com.tripro.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tripro.app.R

/**
 * A plain numeric hour/minute entry dialog — Material3's `TimeInput`, not `TimePicker`'s
 * analog clock-face dial. Two text boxes (+ AM/PM toggle) is faster to use than dragging
 * a clock hand, especially when the user already knows the exact time they want to type
 * (an itinerary item's start/end time, or a hotel's check-in/check-out time).
 *
 * Used anywhere in the app that previously showed the dial-style `TimePicker`: exact/
 * range itinerary item times (AddEditItemSheet), and hotel check-in/check-out
 * (DayDetailScreen's HotelEditDialog).
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
        text = { TimeInput(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}