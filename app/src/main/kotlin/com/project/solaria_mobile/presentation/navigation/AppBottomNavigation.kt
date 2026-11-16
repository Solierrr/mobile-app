package com.project.solaria_mobile.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Elevation
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
internal fun AppBottomNavigation(current: NavDestination?, onSelect: (AppDestination) -> Unit) {
    val entries = listOf(
        Triple("Início", AppIcons.home, AppDestination.Home),
        Triple("Projetos", AppIcons.folder, AppDestination.Projects),
        Triple("Conversas", AppIcons.message_circle, AppDestination.Chats),
    )
    Surface(
        Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        shape = RoundedCornerShape(Radius.pill),
        color = AppTheme.colors.surface,
        shadowElevation = Elevation.floating,
    ) {
        Row(Modifier.fillMaxWidth().padding(Spacing.xxs), horizontalArrangement = Arrangement.SpaceEvenly) {
            entries.forEach { (label, icon, destination) ->
                val active = when (destination) {
                    AppDestination.Home -> current?.hasRoute<AppDestination.Home>() == true
                    AppDestination.Projects -> current?.hasRoute<AppDestination.Projects>() == true
                    else -> current?.hasRoute<AppDestination.Chats>() == true
                }
                Surface(
                    onClick = { if (!active) onSelect(destination) },
                    modifier = Modifier.weight(1f).semantics { selected = active },
                    shape = RoundedCornerShape(Radius.pill),
                    color = if (active) AppTheme.colors.ownMessage else Color.Transparent,
                ) {
                    Column(
                        Modifier.padding(vertical = Spacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        Icon(painterResource(icon), null, Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurface)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) AppTheme.colors.action else AppTheme.colors.onSurfaceMuted)
                    }
                }
            }
        }
    }
}
