package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
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
    color: Color = AppTheme.colors.inputFill,
    online: Boolean = false,
    shape: Shape = CircleShape,
    imageUrl: String? = null,
    loadImage: (suspend (String) -> ByteArray?)? = null,
    textColor: Color = AppTheme.colors.onSurface,
    imageContentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            val fallback: @Composable () -> Unit = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(initials, color = textColor, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                }
            }
            if (imageUrl != null && loadImage != null) {
                RemoteImage(imageUrl, loadImage, null, Modifier.matchParentSize(), contentScale = imageContentScale, placeholder = false, fallback = fallback)
            } else {
                fallback()
            }
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
fun AvatarPalette(index: Int): Color = AppTheme.colors.inputFill
