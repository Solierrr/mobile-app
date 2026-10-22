@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import androidx.compose.material3.MaterialTheme
import com.project.solaria_mobile.core.designsystem.components.ActionTile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.ContentSkeleton
import com.project.solaria_mobile.core.designsystem.components.Skeleton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.presentation.navigation.AppDestination
import kotlinx.coroutines.launch
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.feature.project.domain.ProjectSummary

@Composable
fun ProjectDetailRoute(
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
    sections: ProjectSectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, loading = { ContentSkeleton(cards = true) }, onBack = onBack, header = { ScreenTitle("Projeto", onBack) }) { project -> ProjectDetailScreen(project, sections, onBack, onNavigate, state as? UiState.Content, viewModel::load) }
}

@Composable
fun ProjectDetailScreen(
    project: ProjectSummary,
    sections: ProjectSectionsViewModel,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    resource: UiState.Content<*>? = null,
    onRefresh: () -> Unit = {},
) {
    var activeSheet by remember { mutableStateOf<ProjectSheet?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val open: (ProjectSheet) -> Unit = { target ->
        sections.open(target)
        activeSheet = target
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val photosState by sections.photos.collectAsStateWithLifecycle()
    val coverId = (photosState as? UiState.Content)?.data?.firstOrNull()?.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            if (coverId != null) {
                RemoteImage(
                    key = coverId,
                    load = sections::photoContent,
                    description = project.name,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                if (photosState is UiState.Loading) Skeleton(Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(AppTheme.colors.inputFill))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen)
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(AppIcons.chevron_left, "Voltar", onBack, background = AppTheme.colors.surface.copy(alpha = 0.86f))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    RefreshButton(onRefresh, resource?.refreshing == true, "Atualizar projeto")
                    RoundIconButton(AppIcons.ellipsis, "Mais opções", { menuOpen = true })
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -Spacing.xxl)
                .clip(RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet))
                .background(AppTheme.colors.surface)
                .padding(horizontal = Spacing.screen)
                .padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            Text(project.displayName, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineMedium)
            if (project.companyName.isNotBlank()) Text(project.companyName, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            if (resource != null) ResourceStatus(resource)
            Spacer(Modifier.height(Spacing.xs))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ActionTile("Agenda", AppIcons.calendar_days, { open(ProjectSheet.AGENDA) }, Modifier.weight(1f).fillMaxHeight())
                ActionTile("Envolvidos", AppIcons.users, { open(ProjectSheet.TEAM) }, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ActionTile("Imagens do local", AppIcons.images, { open(ProjectSheet.GALLERY) }, Modifier.weight(1f).fillMaxHeight())
                ActionTile("Conversas", AppIcons.message_circle, { open(ProjectSheet.CONVERSATIONS) }, Modifier.weight(1f).fillMaxHeight())
            }
            Spacer(Modifier.height(Spacing.xs))
            ActionRow("Informações do local", AppIcons.map_pin, { open(ProjectSheet.LOCATION) })
            ActionRow("Histórico do projeto", AppIcons.clock, { open(ProjectSheet.HISTORY) })
            project.companyId?.let { companyId ->
                ActionRow("Empresa responsável", AppIcons.building, { onNavigate(AppDestination.Enterprise(companyId)) })
            }
            ActionRow("Configurações", AppIcons.settings, { onNavigate(AppDestination.Settings) })
        }
    }

    val sheet = activeSheet
    if (sheet != null) {
        BottomSheet(onDismiss = { activeSheet = null }, state = sheetState) {
            ProjectSheetContent(
                sheet = sheet,
                viewModel = sections,
                onBack = { scope.launch { sheetState.hide(); activeSheet = null } },
                onNavigate = { route -> scope.launch { sheetState.hide(); activeSheet = null; onNavigate(route) } },
                onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
            )
        }
    }

    if (menuOpen) {
        BottomSheet(onDismiss = { menuOpen = false }, state = sheetState) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ScreenTitle("Opções", onBack = { menuOpen = false })
                ActionRow("Listar projetos", AppIcons.folder, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.Projects) } })
                ActionRow("Novo projeto", AppIcons.folder_plus, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.NewProject) } })
                ActionRow("Configurações", AppIcons.settings, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.Settings) } })
            }
        }
    }
}
