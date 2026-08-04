package com.tripro.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every value here is copied verbatim from DESIGN.md ("Horizon Ethos" design system).
 * Do not hand-tune these — if the palette changes, change DESIGN.md and regenerate this file.
 */
object HorizonEthosColors {
    val Surface = Color(0xFFF9FAFB)
    val SurfaceDim = Color(0xFFD1D5DB)
    val SurfaceBright = Color(0xFFF9FAFB)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF3F4F6)
    val SurfaceContainer = Color(0xFFE5E7EB)
    val SurfaceContainerHigh = Color(0xFFD1D5DB)
    val SurfaceContainerHighest = Color(0xFF9CA3AF)
    val SurfaceVariant = Color(0xFFF3F4F6)

    val OnSurface = Color(0xFF0B1C30)
    val OnSurfaceVariant = Color(0xFF43474F)
    val InverseSurface = Color(0xFF213145)
    val InverseOnSurface = Color(0xFFEAF1FF)

    val Outline = Color(0xFF747780)
    val OutlineVariant = Color(0xFFC4C6D0)
    val SurfaceTint = Color(0xFF405F91)

    val Primary = Color(0xFF001736) // "Voyage Blue"
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF002B5B)
    val OnPrimaryContainer = Color(0xFF7594CA)
    val InversePrimary = Color(0xFFA9C7FF)

    val Secondary = Color(0xFF7F5600)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFF9AD00) // "Accent Amber"
    val OnSecondaryContainer = Color(0xFF664500)

    val Tertiary = Color(0xFF141819)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFF292C2E)
    val OnTertiaryContainer = Color(0xFF909395)

    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)

    val PrimaryFixed = Color(0xFFD6E3FF)
    val PrimaryFixedDim = Color(0xFFA9C7FF)
    val OnPrimaryFixed = Color(0xFF001B3D)
    val OnPrimaryFixedVariant = Color(0xFF264778)

    val SecondaryFixed = Color(0xFFFFDEAE)
    val SecondaryFixedDim = Color(0xFFFFBA3F)
    val OnSecondaryFixed = Color(0xFF281800)
    val OnSecondaryFixedVariant = Color(0xFF604100)

    val TertiaryFixed = Color(0xFFE0E3E5)
    val TertiaryFixedDim = Color(0xFFC4C7C9)
    val OnTertiaryFixed = Color(0xFF191C1E)
    val OnTertiaryFixedVariant = Color(0xFF444749)

    val Background = Color(0xFFF8F9FF)
    val OnBackground = Color(0xFF0B1C30)

    // Functional palette (from DESIGN.md "Functional Palettes"), not part of the
    // M3 role set but used directly for status pills / alert badges.
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)

    // Card border used throughout the mockups (#E2E8F0), slightly off the
    // generated outline-variant token — kept separate so card borders match exactly.
    val CardBorder = Color(0xFFE2E8F0)
}
