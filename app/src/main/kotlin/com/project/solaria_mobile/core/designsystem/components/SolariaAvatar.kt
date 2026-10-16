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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.SolariaGreen
import com.project.solaria_mobile.core.designsystem.theme.SolariaBlue
import com.project.solaria_mobile.core.designsystem.theme.SolariaPink
import com.project.solaria_mobile.core.designsystem.theme.SolariaOrange
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun SolariaAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = Sizing.avatar,
    color: Color = SolariaOrange,
    online: Boolean = false,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = SolariaWhite,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(SolariaWhite)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SolariaGreen),
            )
        }
    }
}

fun AvatarPalette(index: Int): Color = when (index % 4) {
    0 -> SolariaOrange
    1 -> SolariaBlue
    2 -> SolariaGreen
    else -> SolariaPink
}
