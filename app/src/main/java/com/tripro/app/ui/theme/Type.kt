package com.tripro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tripro.app.R

private val PlusJakartaSansSemiBold = FontFamily(Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold))
private val PlusJakartaSansBold = FontFamily(Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold))
private val WorkSans = FontFamily(Font(R.font.work_sans_regular, FontWeight.Normal))
private val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_medium, FontWeight.Medium))

val TriProTypography = Typography().let { base ->
    base.copy(
        displayLarge = TextStyle(
            fontFamily = PlusJakartaSansBold,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.02).em
        ),
        headlineLarge = TextStyle(
            fontFamily = PlusJakartaSansSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = PlusJakartaSansSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleSmall = TextStyle(
            fontFamily = PlusJakartaSansSemiBold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodySmall = TextStyle(
            fontFamily = WorkSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
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
        labelSmall = TextStyle(
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )
}