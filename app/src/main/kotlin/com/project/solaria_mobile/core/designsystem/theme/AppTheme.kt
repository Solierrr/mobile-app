package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun AppColors.toColorScheme() = lightColorScheme(
    primary = primary,
    onPrimary = onAccent,
    primaryContainer = primaryContainer,
    onPrimaryContainer = brandStrong,
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
    onError = onAccent,
    errorContainer = danger.copy(alpha = 0.12f),
    onErrorContainer = danger,
    tertiary = info,
    onTertiary = onAccent,
    outlineVariant = divider,
    inverseSurface = onSurface,
    inverseOnSurface = surface,
    surfaceContainerLowest = surface,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.small / 2),
    small = RoundedCornerShape(Radius.small),
    medium = RoundedCornerShape(Radius.medium),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.sheet),
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
            shapes = AppShapes,
            content = content,
        )
    }
}
