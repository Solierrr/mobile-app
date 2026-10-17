package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@Composable
fun ListItemDivider(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.fillMaxWidth().height(Sizing.hairline).background(AppTheme.colors.divider))
}

@Composable
fun RowSpacer(width: Dp = Spacing.md) {
    Spacer(modifier = Modifier.width(width))
}
