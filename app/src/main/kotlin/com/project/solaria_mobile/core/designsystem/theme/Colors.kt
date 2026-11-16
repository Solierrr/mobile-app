package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val info: Color,
    val danger: Color,
    val background: Color,
    val surface: Color,
    val onAccent: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceSubtle: Color,
    val inputFill: Color,
    val outline: Color,
    val divider: Color,
    val headerFill: Color,
    val ownMessage: Color,
    val mapWater: Color,
    val mapRoad: Color,
    val mapPark: Color,
    val mapPlaceholder: Color,
)

val LightAppColors = AppColors(
    primary = Color(0xFF202124),
    primaryContainer = Color(0xFFF0F0F2),
    secondary = Color(0xFFFF821C),
    info = Color(0xFF45ADEB),
    danger = Color(0xFFFF3F73),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onAccent = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202124),
    onSurfaceMuted = Color(0xFF686B70),
    onSurfaceSubtle = Color(0xFF8B8E94),
    inputFill = Color(0xFFF4F4F5),
    outline = Color(0xFFE6E7E9),
    divider = Color(0xFFE6E7E9),
    headerFill = Color(0xFFF4F4F5),
    ownMessage = Color(0xFFF0F0F2),
    mapWater = Color(0xFF9AD4F5),
    mapRoad = Color(0xFFFFE999),
    mapPark = Color(0xFFBDEACB),
    mapPlaceholder = Color(0xFFE8E8E8),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
