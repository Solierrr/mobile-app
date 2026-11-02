@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.layout.PaddingValues
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import com.project.solaria_mobile.core.designsystem.components.ListRow
import com.project.solaria_mobile.feature.communication.domain.ServiceCall
import com.project.solaria_mobile.feature.communication.domain.ServiceCallIcon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing

@Composable
internal fun CallCard(call: ServiceCall, onClick: () -> Unit) {
    val icon = when (call.icon) {
        ServiceCallIcon.HASH -> AppIcons.hash
        ServiceCallIcon.PHONE -> AppIcons.phone
        ServiceCallIcon.SHIELD_ALERT -> AppIcons.shield_alert
    }
    ListRow(
        title = call.title,
        subtitle = call.detail,
        subtitleMaxLines = 2,
        onClick = onClick,
        leading = { Icon(painter = painterResource(icon), contentDescription = null, tint = AppTheme.colors.onSurface, modifier = Modifier.size(Sizing.icon)) },
        trailing = call.badge?.let { badge ->
            {
                StatusBadge(badge, tint = when (call.status) { "COMPLETED" -> AppTheme.colors.success; "CANCELED" -> AppTheme.colors.onSurfaceMuted; else -> AppTheme.colors.action })
            }
        },
        container = AppTheme.colors.inputFill,
        contentPadding = PaddingValues(Spacing.md),
    )
}
