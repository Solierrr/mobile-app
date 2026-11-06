package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

/** Pílula translúcida com ícone e título, para flutuar sobre conteúdo como um mapa ou uma lista rolável. */
@Composable
fun FloatingTitle(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    background: Color = AppTheme.colors.surface.copy(alpha = 0.95f),
) {
    Surface(modifier, shape = RoundedCornerShape(Radius.pill), color = background) {
        Row(
            Modifier.height(Sizing.touchTarget).padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
            Text(text, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface, maxLines = 1)
        }
    }
}
