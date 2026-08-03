package com.tripro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.tripro.app.util.AppLanguage

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
)

// DESIGN.md's "fixed" tone tokens (PrimaryFixed, SecondaryFixed, TertiaryFixed, and their
// Dim/On variants) don't have a home in lightColorScheme()'s constructor in every Material3
// version — that API has churned across releases. Rather than depend on a specific version
// having them, HorizonEthosColors.PrimaryFixed etc. (in Color.kt) are just referenced
// directly wherever a component needs that exact value, instead of routing through
// MaterialTheme.colorScheme.

@Composable
fun TriProTheme(appLanguage: AppLanguage = AppLanguage.ENGLISH, content: @Composable () -> Unit) {
    // Layout direction now follows the person's *chosen app language*, not the device's
    // system locale — Compose otherwise mirrors layout to match an RTL device locale
    // regardless of the manifest's supportsRtl flag (what used to cause "everything is
    // flipped" for anyone with an RTL system locale, even though the app only ever spoke
    // English). Language is now an explicit in-app setting, so Hebrew forces Rtl and
    // everything else forces Ltr, decoupled from the device's own locale.
    val layoutDirection = if (appLanguage == AppLanguage.HEBREW) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = HorizonEthosColorScheme,
            typography = TriProTypography,
            shapes = TriProShapes,
            content = content
        )
    }
}
