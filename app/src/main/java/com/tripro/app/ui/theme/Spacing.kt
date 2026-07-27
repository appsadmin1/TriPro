package com.tripro.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * DESIGN.md "Layout & Spacing": all gaps are multiples of 8px ("the 8px rhythm").
 * container-max / margin-desktop are web breakpoint concerns and are not used here
 * since TriPro is a single (phone) form factor.
 */
object TriProSpacing {
    val base = 8.dp
    val stackSm = 4.dp   // related sub-items, e.g. traveler list under a booking
    val stackMd = 12.dp  // between cards in a trip schedule
    val stackLg = 24.dp  // between major sections
    val gutter = 24.dp
    val marginMobile = 16.dp // horizontal safe zone
}
