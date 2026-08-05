package com.tripro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import com.tripro.app.ui.components.TriProAlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProShapes
import com.tripro.app.ui.theme.TriProTypography
import com.tripro.app.R

/**
 * A time entry dialog supporting both scrolling wheels and digital input (TimeInput).
 * Tapping the time header toggles between the two modes.
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
    val initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var showManualInput by remember { mutableStateOf(false) }

    // Recreate state for TimeInput whenever it becomes visible to sync from wheels
    val timeInputState = if (showManualInput) {
        rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = true)
    } else null

    // If in manual mode, sync back to local state
    LaunchedEffect(timeInputState?.hour, timeInputState?.minute) {
        timeInputState?.let {
            selectedHour = it.hour
            selectedMinute = it.minute
        }
    }

    TriProAlertDialog(
        onDismissRequest = onDismiss,
        confirmButtonText = stringResource(R.string.action_ok),
        onConfirm = { onConfirm("%02d:%02d".format(selectedHour, selectedMinute)) },
        dismissButtonText = stringResource(R.string.action_cancel),
        title = title,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clickable Time Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualInput = !showManualInput }
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d:%02d".format(selectedHour, selectedMinute),
                        style = TriProTypography.displayLarge.copy(
                            color = TriProColors.Primary
                        )
                    )
                }

                if (showManualInput && timeInputState != null) {
                    TimeInput(state = timeInputState)
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WheelPicker(
                                count = 24,
                                initialValue = selectedHour,
                                onValueChange = { selectedHour = it },
                                modifier = Modifier.width(70.dp)
                            )
                            Text(
                                ":",
                                style = TriProTypography.displayLarge.copy(
                                    color = TriProColors.Primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            WheelPicker(
                                count = 60,
                                initialValue = selectedMinute,
                                onValueChange = { selectedMinute = it },
                                modifier = Modifier.width(70.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun WheelPicker(
    count: Int,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 48.dp
    // Infinity scrolling effect
    val totalItems = 10000 // Sufficiently large for "infinite" feel
    val initialIndex = totalItems / 2 - (totalItems / 2 % count) + initialValue
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex - 1)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + 1
        }
    }

    LaunchedEffect(centerIndex) {
        onValueChange(centerIndex % count)
    }

    Box(modifier = modifier.height(itemHeight * 3), contentAlignment = Alignment.Center) {
        // Selection overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 4.dp)
                .alpha(0.12f)
                .background(TriProColors.Primary, TriProShapes.small)
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItems) { index ->
                val value = index % count
                val isSelected = index == centerIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "%02d".format(value),
                        style = if (isSelected) {
                            TriProTypography.headlineMedium.copy(
                                color = TriProColors.Primary
                            )
                        } else {
                            TriProTypography.bodyLarge.copy(
                                color = TriProColors.OnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    )
                }
            }
        }
    }
}
