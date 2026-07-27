package com.tripro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * DESIGN.md's token names (surface-container-high, on-primary-fixed-variant, ...) are
 * literally Material 3 ColorScheme role names, because the mockups were produced from an
 * M3 export. That makes this mapping a direct 1:1 transcription rather than an approximation.
 *
 * Horizon Ethos is a light-only design system (see DESIGN.md front matter — no dark palette
 * is specified), so we intentionally expose a single scheme rather than light/dark variants.
 * If/when a dark palette is added to DESIGN.md, add a matching darkColorScheme() here and
 * branch on isSystemInDarkTheme() in TriProTheme below.
 */
private val HorizonEthosColorScheme = lightColorScheme(
    primary = HorizonEthosColors.Primary,
    onPrimary = HorizonEthosColors.OnPrimary,
    primaryContainer = HorizonEthosColors.PrimaryContainer,
    onPrimaryContainer = HorizonEthosColors.OnPrimaryContainer,
    inversePrimary = HorizonEthosColors.InversePrimary,

    secondary = HorizonEthosColors.Secondary,
    onSecondary = HorizonEthosColors.OnSecondary,
    secondaryContainer = HorizonEthosColors.SecondaryContainer,
    onSecondaryContainer = HorizonEthosColors.OnSecondaryContainer,

    tertiary = HorizonEthosColors.Tertiary,
    onTertiary = HorizonEthosColors.OnTertiary,
    tertiaryContainer = HorizonEthosColors.TertiaryContainer,
    onTertiaryContainer = HorizonEthosColors.OnTertiaryContainer,

    background = HorizonEthosColors.Background,
    onBackground = HorizonEthosColors.OnBackground,

    surface = HorizonEthosColors.Surface,
    onSurface = HorizonEthosColors.OnSurface,
    surfaceVariant = HorizonEthosColors.SurfaceVariant,
    onSurfaceVariant = HorizonEthosColors.OnSurfaceVariant,
    surfaceTint = HorizonEthosColors.SurfaceTint,
    surfaceBright = HorizonEthosColors.SurfaceBright,
    surfaceDim = HorizonEthosColors.SurfaceDim,
    surfaceContainer = HorizonEthosColors.SurfaceContainer,
    surfaceContainerLow = HorizonEthosColors.SurfaceContainerLow,
    surfaceContainerLowest = HorizonEthosColors.SurfaceContainerLowest,
    surfaceContainerHigh = HorizonEthosColors.SurfaceContainerHigh,
    surfaceContainerHighest = HorizonEthosColors.SurfaceContainerHighest,

    inverseSurface = HorizonEthosColors.InverseSurface,
    inverseOnSurface = HorizonEthosColors.InverseOnSurface,

    error = HorizonEthosColors.Error,
    onError = HorizonEthosColors.OnError,
    errorContainer = HorizonEthosColors.ErrorContainer,
    onErrorContainer = HorizonEthosColors.OnErrorContainer,

    outline = HorizonEthosColors.Outline,
    outlineVariant = HorizonEthosColors.OutlineVariant,
    scrim = Color.Black,

    primaryFixed = HorizonEthosColors.PrimaryFixed,
    primaryFixedDim = HorizonEthosColors.PrimaryFixedDim,
    onPrimaryFixed = HorizonEthosColors.OnPrimaryFixed,
    onPrimaryFixedVariant = HorizonEthosColors.OnPrimaryFixedVariant,

    secondaryFixed = HorizonEthosColors.SecondaryFixed,
    secondaryFixedDim = HorizonEthosColors.SecondaryFixedDim,
    onSecondaryFixed = HorizonEthosColors.OnSecondaryFixed,
    onSecondaryFixedVariant = HorizonEthosColors.OnSecondaryFixedVariant,

    tertiaryFixed = HorizonEthosColors.TertiaryFixed,
    tertiaryFixedDim = HorizonEthosColors.TertiaryFixedDim,
    onTertiaryFixed = HorizonEthosColors.OnTertiaryFixed,
    onTertiaryFixedVariant = HorizonEthosColors.OnTertiaryFixedVariant,
)

@Composable
fun TriProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HorizonEthosColorScheme,
        typography = TriProTypography,
        shapes = TriProShapes,
        content = content
    )
}
