package com.tripro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.tripro.app.R

/**
 * Premium typography using Google Fonts: Plus Jakarta Sans (Headlines), Work Sans (Body),
 * and JetBrains Mono (Technical).
 */
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val PlusJakartaSans = FontFamily(
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Bold)
)

private val WorkSans = FontFamily(
    Font(googleFont = GoogleFont("Work Sans"), fontProvider = provider),
    Font(googleFont = GoogleFont("Work Sans"), fontProvider = provider, weight = FontWeight.Medium)
)

private val JetBrainsMono = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider)
)

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
