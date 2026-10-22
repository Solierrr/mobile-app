@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.feature.project.domain.ProjectSummary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.theme.Elevation
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.StateNotice

@Composable
fun ProjectListRoute(
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenMap: () -> Unit,
    onCreateProject: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded = state as? UiState.Content
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(loaded?.refreshing) { if (loaded?.refreshing != true) pulling = false }
    PullToRefreshBox(isRefreshing = pulling && loaded?.refreshing == true, onRefresh = { pulling = true; viewModel.load() }) {
        UiStateHost(state, viewModel::load, loading = { ContentSkeleton(cards = true) }, onBack = onBack, header = { ScreenTitle("Projetos", onBack) }) { projects ->
            ProjectListScreen(projects, onBack, onOpenProject, onOpenMap, onCreateProject, loaded, viewModel::load)
        }
    }
}

@Composable
fun ProjectListScreen(
    projects: List<ProjectSummary>,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenMap: () -> Unit,
    onCreateProject: () -> Unit,
    resource: UiState.Content<*>? = null,
    onRefresh: () -> Unit = {},
) {
    val offline = com.project.solaria_mobile.core.offline.LocalOffline.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.screen, vertical = Spacing.screenTop),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item { ScreenTitle("Projetos", onBack, trailingIcon = AppIcons.map_pin, trailingLabel = "Mapa", onTrailingClick = onOpenMap, actions = { RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar projetos") }) }
        item { SectionHeading("Seus projetos", faded = true) }
        if (resource?.refreshError != null && !offline) item { ResourceStatus(resource) }
        if (projects.isEmpty()) item { StateNotice("Nenhum projeto ainda", "Crie um projeto para organizar as atividades, os envolvidos e as informações do local.", icon = AppIcons.folder) }
        items(projects, key = { it.id }) { project -> ProjectListCard(project, onClick = { onOpenProject(project.id) }) }
        item { ActionRow("Criar projeto", AppIcons.folder_plus, onClick = onCreateProject) }
    }
}

@Composable
private fun ProjectListCard(project: ProjectSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Radius.large),
        color = AppTheme.colors.surface,
        shadowElevation = Elevation.flat,
    ) {
        Column {
            ProjectCardImage(project.id, Modifier.fillMaxWidth().height(Sizing.projectCardHeight))
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                Text(project.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(project.status, color = com.project.solaria_mobile.core.designsystem.theme.AppTheme.colors.primary, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                    Text("·", color = AppTheme.colors.onSurfaceMuted)
                    Text(project.people, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
