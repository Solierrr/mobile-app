package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    faded: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    navigation: Boolean = false,
) {
    if (navigation && onAction != null) {
        Row(
            modifier.heightIn(min = Sizing.touchTarget).clickable(onClickLabel = action, role = Role.Button, onClick = onAction),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, modifier = Modifier.weight(1f, fill = false), color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Icon(painterResource(AppIcons.chevron_right), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
        }
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            color = AppTheme.colors.onSurface,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.heightIn(min = Sizing.touchTarget)) {
                Text(action, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            }
        }
    }
}
