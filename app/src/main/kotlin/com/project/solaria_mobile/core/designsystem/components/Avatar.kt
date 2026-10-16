package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = Sizing.avatar,
    color: Color = AppTheme.colors.secondary,
    online: Boolean = false,
    shape: Shape = CircleShape,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = AppTheme.colors.onAccent,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(AppTheme.colors.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.primary),
            )
        }
    }
}

@Composable
fun AvatarPalette(index: Int): Color = when (index % 4) {
    0 -> AppTheme.colors.secondary
    1 -> AppTheme.colors.info
    2 -> AppTheme.colors.primary
    else -> AppTheme.colors.danger
}
