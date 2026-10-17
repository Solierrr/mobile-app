package com.project.solaria_mobile.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
fun ActionRow(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    trailing: String? = null,
    subtitle: String? = null,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Radius.pill),
        color = AppTheme.colors.inputFill,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = if (compact) 42.dp else Sizing.actionHeight)
                .padding(horizontal = if (compact) Spacing.md else 22.dp, vertical = if (compact) 8.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = AppTheme.colors.onSurface,
                modifier = Modifier.size(if (compact) Sizing.iconSmall else Sizing.icon),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    color = AppTheme.colors.onSurface,
                    style = if (compact) androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    else androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = AppTheme.colors.onSurfaceMuted,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when {
                badge != null -> CountBadge(value = badge)
                trailing != null -> Text(
                    text = trailing,
                    color = AppTheme.colors.onSurfaceMuted,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
