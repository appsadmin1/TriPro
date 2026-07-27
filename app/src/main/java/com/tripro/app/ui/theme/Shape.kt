package com.tripro.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DESIGN.md "Shapes": standard elements 8px, large containers 16px, indicators fully round.
 */
val TriProShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),  // rounded.sm
    small = RoundedCornerShape(8.dp),       // rounded.DEFAULT — buttons, inputs, small cards
    medium = RoundedCornerShape(12.dp),     // rounded.md
    large = RoundedCornerShape(16.dp),      // rounded.lg — trip overview sections, hero images
    extraLarge = RoundedCornerShape(24.dp)  // rounded.xl
)

/** Pills / avatars / status badges use full round per DESIGN.md "Indicators". */
val PillShape = RoundedCornerShape(percent = 50)
