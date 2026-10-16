@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.data.ServiceCall
import com.project.solaria_mobile.feature.communication.data.ServiceCallIcon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
internal fun CallCard(call: ServiceCall, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Radius.medium),
        color = AppTheme.colors.inputFill,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (call.icon) {
                ServiceCallIcon.HASH -> LucideR.drawable.lucide_ic_hash
                ServiceCallIcon.PHONE -> LucideR.drawable.lucide_ic_phone
                ServiceCallIcon.SHIELD_ALERT -> LucideR.drawable.lucide_ic_shield_alert
            }
            Icon(painter = painterResource(icon), contentDescription = null, tint = AppTheme.colors.onSurface, modifier = Modifier.size(Sizing.icon))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(call.title, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text(call.detail, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (call.badge != null) {
                Text(call.badge, color = if (call.badge == "!!") AppTheme.colors.danger else AppTheme.colors.primary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            }
        }
    }
}
