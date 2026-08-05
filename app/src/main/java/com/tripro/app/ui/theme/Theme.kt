package com.tripro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tripro.app.util.AppLanguage

/**
 * DESIGN.md's token names (surface-container-high, on-primary-fixed-variant, ...) are
 * literally Material 3 ColorScheme role names, because the mockups were produced from an
 * M3 export. That makes this mapping a direct 1:1 transcription rather than an approximation.
 *
 * TriPro is a light-only design system (see DESIGN.md front matter — no dark palette
 * is specified), so we intentionally expose a single scheme rather than light/dark variants.
 * If/when a dark palette is added to DESIGN.md, add a matching darkColorScheme() here and
 * branch on isSystemInDarkTheme() in TriProTheme below.
 */
private val TriProColorScheme = lightColorScheme(
    primary = TriProColors.Primary,
    onPrimary = TriProColors.OnPrimary,
    primaryContainer = TriProColors.PrimaryContainer,
    onPrimaryContainer = TriProColors.OnPrimaryContainer,
    inversePrimary = TriProColors.InversePrimary,

    secondary = TriProColors.Secondary,
    onSecondary = TriProColors.OnSecondary,
    secondaryContainer = TriProColors.SecondaryContainer,
    onSecondaryContainer = TriProColors.OnSecondaryContainer,

    tertiary = TriProColors.Tertiary,
    onTertiary = TriProColors.OnTertiary,
    tertiaryContainer = TriProColors.TertiaryContainer,
    onTertiaryContainer = TriProColors.OnTertiaryContainer,

    background = TriProColors.Background,
    onBackground = TriProColors.OnBackground,

    surface = TriProColors.Surface,
    onSurface = TriProColors.OnSurface,
    surfaceVariant = TriProColors.SurfaceVariant,
    onSurfaceVariant = TriProColors.OnSurfaceVariant,
    surfaceTint = TriProColors.SurfaceTint,
    surfaceBright = TriProColors.SurfaceBright,
    surfaceDim = TriProColors.SurfaceDim,
    surfaceContainer = TriProColors.SurfaceContainer,
    surfaceContainerLow = TriProColors.SurfaceContainerLow,
    surfaceContainerLowest = TriProColors.SurfaceContainerLowest,
    surfaceContainerHigh = TriProColors.SurfaceContainerHigh,
    surfaceContainerHighest = TriProColors.SurfaceContainerHighest,

    inverseSurface = TriProColors.InverseSurface,
    inverseOnSurface = TriProColors.InverseOnSurface,

    error = TriProColors.Error,
    onError = TriProColors.OnError,
    errorContainer = TriProColors.ErrorContainer,
    onErrorContainer = TriProColors.OnErrorContainer,

    outline = TriProColors.Outline,
    outlineVariant = TriProColors.OutlineVariant,
    scrim = Color.Black,
)

// DESIGN.md's "fixed" tone tokens (PrimaryFixed, SecondaryFixed, TertiaryFixed, and their
// Dim/On variants) don't have a home in lightColorScheme()'s constructor in every Material3
// version — that API has churned across releases. Rather than depend on a specific version
// having them, TriProColors.PrimaryFixed etc. (in Color.kt) are just referenced
// directly wherever a component needs that exact value, instead of routing through
// MaterialTheme.colorScheme.

@Composable
fun TriProTheme(content: @Composable () -> Unit) {
    // Layout direction and resources are now correctly driven by the Activity context 
    // wrapping in MainActivity.attachBaseContext(). Removing manual layout direction 
    // overrides to ensure we are testing the true system-applied configuration.
    MaterialTheme(
        colorScheme = TriProColorScheme,
        typography = TriProTypography,
        shapes = TriProShapes,
        content = content
    )
}
