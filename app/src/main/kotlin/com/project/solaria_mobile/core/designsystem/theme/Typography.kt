package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.project.solaria_mobile.R

private val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

private val pageTitle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp)
private val sectionTitle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp)
private val itemTitle = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp)
private val body = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp)
private val supporting = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp)

val AppTypography = Typography(
    displayLarge = pageTitle, displayMedium = pageTitle, displaySmall = pageTitle,
    headlineLarge = pageTitle, headlineMedium = pageTitle, headlineSmall = pageTitle,
    titleLarge = sectionTitle, titleMedium = sectionTitle, titleSmall = itemTitle,
    bodyLarge = body, bodyMedium = supporting, bodySmall = supporting,
    labelLarge = supporting, labelMedium = supporting, labelSmall = supporting,
)
