package com.project.solaria_mobile.presentation.navigation

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
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
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
internal fun AppBottomNavigation(current: NavDestination?, glass: HazeState, onSelect: (AppDestination) -> Unit) {
    val entries = listOf(
        Triple(stringResource(R.string.nv_home), AppIcons.home, AppDestination.Home),
        Triple(stringResource(R.string.pl_projetos), AppIcons.folder, AppDestination.Projects),
        Triple(stringResource(R.string.pd_conversas), AppIcons.message_circle, AppDestination.Chats),
    )
    val pill = RoundedCornerShape(Radius.pill)
    // Vidro fosco: o conteúdo da página passa por baixo, desfocado, com um véu claro por cima.
    Box(
        Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xs)
            .clip(pill)
            .hazeEffect(glass) {
                blurRadius = 24.dp
                tints = listOf(HazeTint(Color.White.copy(alpha = 0.5f)))
                noiseFactor = 0.02f
            }
            .border(1.dp, Color.White.copy(alpha = 0.7f), pill),
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
                    color = if (active) Color.Black.copy(alpha = 0.07f) else Color.Transparent,
                ) {
                    Column(
                        Modifier.padding(vertical = Spacing.xs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        Icon(painterResource(icon), null, Modifier.size(Sizing.icon), tint = if (active) AppTheme.colors.action else AppTheme.colors.onSurface)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) AppTheme.colors.brandStrong else AppTheme.colors.onSurfaceMuted)
                    }
                }
            }
        }
    }
}
