package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun AppColors.toColorScheme() = lightColorScheme(
    primary = primary,
    onPrimary = onAccent,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onSurface,
    secondary = secondary,
    onSecondary = onAccent,
    secondaryContainer = inputFill,
    onSecondaryContainer = onSurface,
    background = background,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = inputFill,
    surfaceContainer = surface,
    surfaceContainerLow = background,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = inputFill,
    surfaceTint = primary,
    onSurfaceVariant = onSurfaceMuted,
    outline = outline,
    error = danger,
)

object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

@Composable
fun AppTheme(
    colors: AppColors = LightAppColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = AppTypography,
            content = content,
        )
    }
}
