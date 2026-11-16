@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.feature.project.domain.ProjectSummary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.theme.Elevation
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.theme.appBackground
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
import com.project.solaria_mobile.feature.project.domain.ProjectInvitation
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton

@Composable
fun ProjectListRoute(
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenMap: () -> Unit,
    onCreateProject: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridMode by viewModel.gridMode.collectAsStateWithLifecycle()
    val layoutFailure by viewModel.layoutFailure.collectAsStateWithLifecycle()
    val invitations by viewModel.invitations.collectAsStateWithLifecycle()
    val invitationAction by viewModel.invitationAction.collectAsStateWithLifecycle()
    val loaded = state as? UiState.Content
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(loaded?.refreshing) { if (loaded?.refreshing != true) pulling = false }
    PullToRefreshBox(isRefreshing = pulling && loaded?.refreshing == true, onRefresh = { pulling = true; viewModel.load() }) {
        UiStateHost(state, viewModel::load, loading = { ProjectListSkeleton() }, onBack = onBack, header = { ScreenTitle("Projetos", onBack = null) }) { projects ->
            ProjectListScreen(projects, onBack, onOpenProject, onOpenMap, onCreateProject, loaded, viewModel::load,
                gridMode, viewModel::toggleLayout, layoutFailure, invitations, invitationAction, viewModel::respond)
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
    gridMode: Boolean = false,
    onToggleLayout: () -> Unit = {},
    layoutFailure: Boolean = false,
    invitations: List<ProjectInvitation> = emptyList(),
    invitationAction: ProjectAction? = null,
    onRespondInvitation: (String, Boolean) -> Unit = { _, _ -> },
) {
    val offline = com.project.solaria_mobile.core.offline.LocalOffline.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (gridMode) 2 else 1),
        modifier = Modifier.fillMaxSize().appBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.screen, vertical = Spacing.screenTop),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ScreenTitle("Projetos", onBack = null)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    SectionHeading("Seus projetos", Modifier.weight(1f), faded = true)
                    RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar projetos")
                    RoundIconButton(if (gridMode) AppIcons.layout_list else AppIcons.layout_grid,
                        if (gridMode) "Mostrar lista compacta" else "Mostrar grade", onToggleLayout)
                    RoundIconButton(AppIcons.map_pin, "Mapa dos projetos", onOpenMap)
                }
            }
        }
        if (invitations.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeading("Convites recebidos", faded = true)
                invitations.forEach { invitation -> InvitationCard(invitation, invitationAction != ProjectAction.Working, onRespondInvitation) }
                ProjectActionStatus(invitationAction, working = "Respondendo…")
            }
        }
        if (layoutFailure) item(span = { GridItemSpan(maxLineSpan) }) { Text("Não foi possível salvar a visualização. Tente novamente.", color = AppTheme.colors.danger) }
        if (resource?.refreshError != null && !offline) item(span = { GridItemSpan(maxLineSpan) }) { ResourceStatus(resource) }
        if (projects.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { StateNotice("Nenhum projeto ainda", "Crie um projeto para organizar as atividades, os envolvidos e as informações do local.", icon = AppIcons.folder) }
        items(projects, key = { it.id }) { project -> ProjectListCard(project, gridMode, onClick = { onOpenProject(project.id) }) }
        item(span = { GridItemSpan(maxLineSpan) }) { ActionRow("Criar projeto", AppIcons.folder_plus, onClick = onCreateProject) }
    }
}

@Composable
private fun ProjectListCard(project: ProjectSummary, grid: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(Radius.large),
        color = AppTheme.colors.surface,
        shadowElevation = Elevation.flat,
    ) {
        if (grid) Column {
            ProjectCardImage(project.id, Modifier.fillMaxWidth().height(Sizing.projectCardHeight))
            ProjectCardSummary(project, Modifier.padding(Spacing.md), titleLines = 2)
        } else Row(Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ProjectCardImage(project.id, Modifier.size(Sizing.avatarLarge).clip(RoundedCornerShape(Radius.medium)))
            ProjectCardSummary(project, Modifier.weight(1f), titleLines = 1)
        }
    }
}

@Composable
private fun ProjectCardSummary(project: ProjectSummary, modifier: Modifier, titleLines: Int) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(project.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, minLines = titleLines, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (project.place.isNotBlank()) Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        StatusBadge(project.status, tint = projectStatusTint(project.status))
    }
}

@Composable
private fun ProjectListSkeleton() {
    Column(Modifier.fillMaxWidth().padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Skeleton(Modifier.fillMaxWidth(0.5f).height(Sizing.headerButton))
        repeat(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Skeleton(Modifier.size(Sizing.avatarLarge))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Skeleton(Modifier.fillMaxWidth().height(Spacing.lg))
                    Skeleton(Modifier.fillMaxWidth(0.7f).height(Spacing.md))
                    Skeleton(Modifier.fillMaxWidth(0.4f).height(Spacing.md))
                }
            }
        }
    }
}

/** Convite para participar de um projeto, com as duas respostas possíveis. */
@Composable
private fun InvitationCard(invitation: ProjectInvitation, enabled: Boolean, onRespond: (String, Boolean) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.surface) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(invitation.projectName, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Text(listOf(invitation.role, invitation.expires).filter { it.isNotBlank() }.joinToString(" · "),
                color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton("Recusar", { onRespond(invitation.id, false) }, Modifier.weight(1f), enabled = enabled, secondary = true, compact = true)
                PrimaryButton("Aceitar", { onRespond(invitation.id, true) }, Modifier.weight(1f), enabled = enabled, compact = true)
            }
        }
    }
}
