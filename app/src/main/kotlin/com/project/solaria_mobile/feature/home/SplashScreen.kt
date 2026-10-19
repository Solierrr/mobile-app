package com.project.solaria_mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.components.BrandLoader
import com.project.solaria_mobile.core.designsystem.components.SPLASH_WIDTH_FRACTION
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

@Composable
fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface), contentAlignment = Alignment.Center) {
        BrandLoader(widthFraction = SPLASH_WIDTH_FRACTION)
    }
}
