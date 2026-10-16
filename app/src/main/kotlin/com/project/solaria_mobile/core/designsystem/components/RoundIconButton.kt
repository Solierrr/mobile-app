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
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaPink
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizing.headerButton,
    tint: Color = SolariaInk,
    background: Color = SolariaWhite,
    badge: String? = null,
) {
    Box(modifier = modifier.size(size)) {
        Surface(
            modifier = Modifier.matchParentSize(),
            onClick = onClick,
            shape = CircleShape,
            color = background,
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(Sizing.icon),
                )
            }
        }
        if (badge != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                shape = CircleShape,
                color = SolariaPink,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        color = SolariaWhite,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
