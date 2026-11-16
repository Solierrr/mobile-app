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
import com.project.solaria_mobile.core.designsystem.components.SkeletonGroup
import com.project.solaria_mobile.core.designsystem.components.SkeletonText
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.SheetValue
import com.project.solaria_mobile.core.designsystem.components.SheetSize
import androidx.compose.runtime.LaunchedEffect
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption
import androidx.compose.material3.TextButton
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton

@Composable
fun ProjectDetailRoute(
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    updated: Boolean = false,
    onUpdateHandled: () -> Unit = {},
    viewModel: ProjectDetailViewModel = hiltViewModel(),
    sections: ProjectSectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    LaunchedEffect(action) { if (action == ProjectAction.Archived) onBack() }
    LaunchedEffect(updated) {
        if (updated) {
            viewModel.load()
            onUpdateHandled()
        }
    }
    UiStateHost(state, viewModel::load, loading = { ProjectDetailSkeleton() }, loadingHeader = false, onBack = onBack, header = { ScreenTitle("Projeto", onBack) }) { project -> ProjectDetailScreen(project, sections, onBack, onNavigate, state as? UiState.Content, viewModel::load, action, viewModel::changeStatus, viewModel::archive, viewModel::clearAction) }
}

@Composable
fun ProjectDetailScreen(
    project: ProjectSummary,
    sections: ProjectSectionsViewModel,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    resource: UiState.Content<*>? = null,
    onRefresh: () -> Unit = {},
    action: ProjectAction? = null,
    onChangeStatus: (ProjectStatusOption) -> Unit = {},
    onArchive: () -> Unit = {},
    onClearAction: () -> Unit = {},
) {
    var activeSheet by rememberSaveable { mutableStateOf<ProjectSheet?>(null) }
    // Segundo nível da sheet aberta (foto da galeria ou pessoa dos envolvidos).
    var sheetDetail by rememberSaveable { mutableStateOf<String?>(null) }
    val currentDetail by rememberUpdatedState(sheetDetail)
    var menuOpen by remember { mutableStateOf(false) }
    var menuStep by remember { mutableStateOf(MenuStep.MAIN) }
    // A situação nova só aparece depois da confirmação do servidor; aí o menu fecha.
    LaunchedEffect(project.status) { if (menuStep == MenuStep.STATUS) menuOpen = false }
    val open: (ProjectSheet) -> Unit = { target ->
        sheetDetail = null
        activeSheet = target
    }
    // Carrega a seção ao abrir e também quando a sheet é restaurada depois de a tela ser recriada.
    LaunchedEffect(activeSheet) { activeSheet?.let(sections::open) }
    // Com o segundo nível aberto, voltar, tocar fora ou arrastar só retorna ao primeiro nível.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (value == SheetValue.Hidden && currentDetail != null) {
            sheetDetail = null
            false
        } else true
    })
    val levelOneScroll = rememberScrollState()
    val levelTwoScroll = rememberScrollState()
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
                else Box(Modifier.fillMaxSize().background(AppTheme.colors.inputFill), contentAlignment = Alignment.Center) {
                    Icon(painterResource(AppIcons.images), "Sem imagem cadastrada", Modifier.size(Sizing.icon), tint = AppTheme.colors.onSurfaceMuted)
                }
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
                    RoundIconButton(AppIcons.ellipsis, "Mais opções", { onClearAction(); menuStep = MenuStep.MAIN; menuOpen = true })
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
        BottomSheet(
            onDismiss = { activeSheet = null; sheetDetail = null },
            state = sheetState,
            size = if (sheet == ProjectSheet.GALLERY) SheetSize.Expanded else SheetSize.Content,
            scrollState = if (sheetDetail == null) levelOneScroll else levelTwoScroll,
        ) {
            ProjectSheetContent(
                sheet = sheet,
                viewModel = sections,
                onNavigate = { route -> scope.launch { sheetDetail = null; sheetState.hide(); activeSheet = null; onNavigate(route) } },
                onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
                detail = sheetDetail,
                onOpenDetail = { key -> scope.launch { levelTwoScroll.scrollTo(0) }; sheetDetail = key },
                onCloseDetail = { sheetDetail = null },
            )
        }
    }

    if (menuOpen) {
        BottomSheet(onDismiss = { menuOpen = false }, state = sheetState, size = SheetSize.Compact) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                val close: (AppDestination) -> Unit = { route -> scope.launch { sheetState.hide(); menuOpen = false; onNavigate(route) } }
                when (menuStep) {
                    MenuStep.MAIN -> {
                        ActionRow("Alterar situação", AppIcons.check, { onClearAction(); menuStep = MenuStep.STATUS }, trailing = project.status.ifBlank { null })
                        ActionRow("Editar projeto", AppIcons.pencil, { close(AppDestination.EditProject(project.id)) })
                        ActionRow("Listar projetos", AppIcons.folder, { close(AppDestination.Projects) })
                        ActionRow("Novo projeto", AppIcons.folder_plus, { close(AppDestination.NewProject) })
                        ActionRow("Configurações", AppIcons.settings, { close(AppDestination.Settings) })
                        ActionRow("Arquivar projeto", AppIcons.folder, { onClearAction(); menuStep = MenuStep.ARCHIVE })
                    }
                    MenuStep.STATUS -> {
                        Text("Situação do projeto", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.onSurface)
                        ProjectStatusOption.entries.forEach { option ->
                            val current = option.label == project.status
                            ActionRow(option.label, if (current) AppIcons.check else AppIcons.clock, { if (!current) onChangeStatus(option) },
                                trailing = if (current) "Atual" else null, compact = true)
                        }
                        ProjectActionStatus(action, working = "Salvando…")
                        TextButton({ menuStep = MenuStep.MAIN }) { Text("Voltar", color = AppTheme.colors.onSurfaceMuted) }
                    }
                    MenuStep.ARCHIVE -> {
                        Text("Arquivar este projeto?", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.onSurface)
                        Text("O projeto sai da sua lista. As informações continuam guardadas pela empresa responsável.",
                            style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                        ProjectActionStatus(action, working = "Arquivando…")
                        PrimaryButton("Arquivar projeto", onArchive, enabled = action != ProjectAction.Working)
                        PrimaryButton("Cancelar", { menuStep = MenuStep.MAIN }, secondary = true)
                    }
                }
            }
        }
    }
}

/** Foto no topo e painel sobreposto com título, atalhos e ações, como no detalhe carregado. */
@Composable
private fun ProjectDetailSkeleton() {
    Column(Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        SkeletonGroup(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Skeleton(Modifier.fillMaxWidth().height(240.dp), RoundedCornerShape(0.dp))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SkeletonText(0.3f)
                SkeletonText(0.7f, height = Spacing.xl)
                SkeletonText(0.45f)
                repeat(2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        repeat(2) { Skeleton(Modifier.weight(1f).height(TILE_SKELETON_HEIGHT)) }
                    }
                }
                repeat(3) { Skeleton(Modifier.fillMaxWidth().height(Sizing.actionHeight)) }
            }
        }
    }
}

private val TILE_SKELETON_HEIGHT = 88.dp

private enum class MenuStep { MAIN, STATUS, ARCHIVE }

/** Andamento ou recusa da operação em curso, logo abaixo das opções. */
@Composable
internal fun ProjectActionStatus(action: ProjectAction?, working: String) {
    when (action) {
        ProjectAction.Working -> Text(working, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
        is ProjectAction.Failed -> Text(action.message(), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.danger)
        else -> Unit
    }
}
