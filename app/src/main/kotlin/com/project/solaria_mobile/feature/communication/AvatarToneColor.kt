package com.project.solaria_mobile.feature.communication

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.feature.communication.domain.AvatarTone

@Composable
fun AvatarTone.toColor(): Color = when (this) {
    AvatarTone.ORANGE -> AppTheme.colors.inputFill
    AvatarTone.BLUE -> AppTheme.colors.inputFill
    AvatarTone.GREEN -> AppTheme.colors.inputFill
    AvatarTone.PINK -> AppTheme.colors.danger
}
