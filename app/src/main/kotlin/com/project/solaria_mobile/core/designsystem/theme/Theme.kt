package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SolariaColorScheme = lightColorScheme(
    primary = SolariaGreen,
    onPrimary = SolariaWhite,
    secondary = SolariaOrange,
    onSecondary = SolariaWhite,
    background = SolariaCanvas,
    onBackground = SolariaInk,
    surface = SolariaWhite,
    onSurface = SolariaInk,
    surfaceVariant = SolariaInput,
    onSurfaceVariant = SolariaInkMuted,
    outline = SolariaLine,
    error = SolariaPink,
)

@Composable
fun SolariamobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SolariaColorScheme,
        typography = SolariaTypography,
        content = content,
    )
}
