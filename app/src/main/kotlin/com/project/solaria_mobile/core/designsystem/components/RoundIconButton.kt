package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizing.headerButton,
    tint: Color = AppTheme.colors.onSurface,
    background: Color = AppTheme.colors.inputFill,
    badge: String? = null,
    enabled: Boolean = true,
) {
    Box(modifier = modifier.size(size.coerceAtLeast(Sizing.touchTarget))) {
        Surface(
            modifier = Modifier.matchParentSize(),
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = background,
            shadowElevation = com.project.solaria_mobile.core.designsystem.theme.Elevation.flat,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = contentDescription,
                    tint = if (enabled) tint else tint.copy(alpha = 0.45f),
                    modifier = Modifier.size(Sizing.icon),
                )
            }
        }
        if (badge != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                shape = CircleShape,
                color = AppTheme.colors.danger,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        color = AppTheme.colors.onAccent,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
