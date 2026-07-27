package com.tripro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * DESIGN.md specifies three families: Plus Jakarta Sans (headlines), Work Sans (body),
 * and JetBrains Mono (technical labels / timestamps / confirmation codes).
 *
 * To use the real webfonts instead of the system default:
 *  1. Download the static .ttf/.otf files from https://fonts.google.com
 *  2. Drop them into app/src/main/res/font/ (e.g. plus_jakarta_sans_semibold.ttf)
 *  3. Replace FontFamily.Default below with FontFamily(Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold), ...)
 *
 * Leaving them as FontFamily.Default keeps this project buildable with zero extra
 * setup; swapping in the real fonts is a five-minute follow-up, not a blocker.
 */
private val PlusJakartaSans = FontFamily.Default
private val WorkSans = FontFamily.Default
private val JetBrainsMono = FontFamily.Default

val TriProTypography = Typography().let { base ->
    base.copy(
        displayLarge = TextStyle(
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.02).em
        ),
        headlineLarge = TextStyle( // headline-lg-mobile in DESIGN.md — this is a phone app, so mobile scale is the default scale
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle( // headline-md
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        bodyLarge = TextStyle( // body-lg
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp
        ),
        bodyMedium = TextStyle( // body-md
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodySmall = TextStyle( // body-sm
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle( // label-md, used for buttons ("Share", "Send Invite", etc.)
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.05.em
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.05.em
        ),
        labelSmall = TextStyle( // label-sm, used for metadata tags: "GATE", "4 DAYS AWAY"
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}
