@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.core.designsystem.components.localizedStatus
import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
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
import com.project.solaria_mobile.core.designsystem.components.MissingImage
import com.project.solaria_mobile.core.designsystem.components.AppDialog
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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.saveable.rememberSaveable
import com.project.solaria_mobile.core.designsystem.components.SheetSize
import com.project.solaria_mobile.core.designsystem.components.rememberSheetState
import androidx.compose.runtime.LaunchedEffect
import com.project.solaria_mobile.feature.project.domain.ProjectStatusOption

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
    UiStateHost(state, viewModel::load, loading = { ProjectDetailSkeleton() }, loadingHeader = false, onBack = onBack, header = { ScreenTitle(stringResource(R.string.specialty_project), onBack) }) { project -> ProjectDetailScreen(project, sections, onBack, onNavigate, state as? UiState.Content, viewModel::load, action, viewModel::changeStatus, viewModel::archive, viewModel::clearAction) }
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
    // Terceiro nível: formulário de edição da atividade, empilhado sobre o detalhe.
    var sheetEdit by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var menuStep by remember { mutableStateOf(MenuStep.MAIN) }
    // A situação nova só aparece depois da confirmação do servidor; aí o menu fecha.
    LaunchedEffect(project.status) { if (menuStep == MenuStep.STATUS) { menuOpen = false; menuStep = MenuStep.MAIN } }
    val open: (ProjectSheet) -> Unit = { target ->
        sheetDetail = null
        sheetEdit = null
        activeSheet = target
    }
    // Carrega a seção ao abrir e também quando a sheet é restaurada depois de a tela ser recriada.
    LaunchedEffect(activeSheet) { activeSheet?.let(sections::open) }
    val sheetState = rememberSheetState()
    val levelOneScroll = rememberScrollState()
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
        var coverFailed by remember { mutableStateOf(false) }
        // Sem imagem na capa, os botões ficam sólidos em vez de translúcidos, que sumiriam no fundo claro.
        val noCover = coverFailed || (coverId == null && photosState !is UiState.Loading)
        val coverButton = if (noCover) AppTheme.colors.surface else AppTheme.colors.surface.copy(alpha = 0.86f)
        Box(modifier = Modifier.fillMaxWidth().height(Sizing.coverHeight)) {
            if (coverId != null) {
                RemoteImage(
                    key = coverId,
                    load = sections::photoContent,
                    description = project.name,
                    modifier = Modifier.fillMaxSize(),
                    onMissing = { coverFailed = it },
                )
            } else {
                if (photosState is UiState.Loading) Skeleton(Modifier.fillMaxSize())
                else MissingImage(Modifier.fillMaxSize())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen)
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(AppIcons.chevron_left, stringResource(R.string.common_back), onBack, background = coverButton)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    // Sobre a foto os dois ficam no cinza padrão; sem foto, sólidos como o de voltar.
                    RefreshButton(onRefresh, resource?.refreshing == true, stringResource(R.string.pd_atualizar_projeto), background = if (noCover) coverButton else AppTheme.colors.inputFill)
                    RoundIconButton(AppIcons.ellipsis, stringResource(R.string.pd_mais_opcoes), { onClearAction(); menuStep = MenuStep.MAIN; menuOpen = true }, background = if (noCover) coverButton else AppTheme.colors.inputFill)
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                Text(project.displayName, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.headlineMedium)
                if (project.companyName.isNotBlank()) Text(project.companyName, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
            }
            if (resource != null) ResourceStatus(resource)
            Spacer(Modifier.height(Spacing.xs))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ActionTile(stringResource(R.string.pd_agenda), AppIcons.calendar_days, { open(ProjectSheet.AGENDA) }, Modifier.weight(1f).fillMaxHeight())
                ActionTile(stringResource(R.string.pd_envolvidos), AppIcons.users, { open(ProjectSheet.TEAM) }, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ActionTile(stringResource(R.string.pd_imagens_do_local), AppIcons.images, { open(ProjectSheet.GALLERY) }, Modifier.weight(1f).fillMaxHeight())
                ActionTile(stringResource(R.string.pd_conversas), AppIcons.message_circle, { open(ProjectSheet.CONVERSATIONS) }, Modifier.weight(1f).fillMaxHeight())
            }
            ActionRow(stringResource(R.string.address_section_title), AppIcons.map_pin, { open(ProjectSheet.LOCATION) })
            ActionRow(stringResource(R.string.pd_historico_do_projeto), AppIcons.clock, { open(ProjectSheet.HISTORY) })
            ActionRow(stringResource(R.string.pd_novo_relatorio), AppIcons.pencil, { open(ProjectSheet.REPORT) })
            project.companyId?.let { companyId ->
                ActionRow(stringResource(R.string.pd_empresa_responsavel), AppIcons.building, { onNavigate(AppDestination.Enterprise(companyId)) })
            }
            ActionRow(stringResource(R.string.settings_title), AppIcons.settings, { onNavigate(AppDestination.Settings) })
        }
    }

    val sheet = activeSheet
    val leaveSheet: (AppDestination) -> Unit = { route -> scope.launch { sheetDetail = null; sheetEdit = null; sheetState.hide(); activeSheet = null; onNavigate(route) } }
    if (sheet != null) {
        BottomSheet(
            onDismiss = { activeSheet = null; sheetDetail = null; sheetEdit = null },
            state = sheetState,
            size = if (sheet == ProjectSheet.GALLERY) SheetSize.Expanded else SheetSize.Content,
            scrollState = levelOneScroll,
        ) {
            ProjectSheetContent(
                sheet = sheet,
                viewModel = sections,
                onNavigate = leaveSheet,
                onCopy = { text -> clipboard.setText(AnnotatedString(text)) },
                onOpenDetail = { key -> sheetDetail = key },
            )
        }
        // Foto, atividade, pessoa ou convite abrem empilhados sobre a lista, que continua aberta por baixo.
        sheetDetail?.let { key ->
            BottomSheet(onDismiss = { sheetDetail = null; sheetEdit = null }, size = SheetSize.Content) {
                val photoId = photoIdOf(key)
                if (photoId != null) PhotoSheetContent(photoId, sections)
                else SheetDetailContent(key, sections, leaveSheet) { next -> if (isActivityForm(next) || photoIdOf(next) != null) sheetEdit = next else sheetDetail = next }
            }
        }
        // A edição da atividade abre numa terceira sheet por cima do detalhe, em vez de substituí-lo.
        sheetEdit?.let { key ->
            BottomSheet(onDismiss = { sheetEdit = null }, size = SheetSize.Content) {
                val photoId = photoIdOf(key)
                if (photoId != null) PhotoSheetContent(photoId, sections) else SheetDetailContent(key, sections, leaveSheet) { }
            }
        }
    }

    if (menuOpen) {
        BottomSheet(onDismiss = { menuOpen = false; menuStep = MenuStep.MAIN }, state = sheetState, size = SheetSize.Compact) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                val close: (AppDestination) -> Unit = { route -> scope.launch { sheetState.hide(); menuOpen = false; onNavigate(route) } }
                ActionRow(stringResource(R.string.pd_alterar_situacao), AppIcons.check, { onClearAction(); menuStep = MenuStep.STATUS }, trailing = project.status.ifBlank { null }?.let { localizedStatus(it) })
                ActionRow(stringResource(R.string.pd_editar_projeto), AppIcons.pencil, { close(AppDestination.EditProject(project.id)) })
                ActionRow(stringResource(R.string.pd_listar_projetos), AppIcons.folder, { close(AppDestination.Projects) })
                ActionRow(stringResource(R.string.pd_novo_projeto), AppIcons.folder_plus, { close(AppDestination.NewProject) })
                ActionRow(stringResource(R.string.settings_title), AppIcons.settings, { close(AppDestination.Settings) })
                ActionRow(stringResource(R.string.pd_arquivar_projeto), AppIcons.folder, { onClearAction(); menuStep = MenuStep.ARCHIVE })
            }
        }
        // A situação abre empilhada sobre o menu e o arquivamento pede confirmação num popup; o menu nunca troca de conteúdo.
        if (menuStep == MenuStep.STATUS) {
            BottomSheet(onDismiss = { menuStep = MenuStep.MAIN }, size = SheetSize.Content) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text(stringResource(R.string.pd_situacao_do_projeto), style = MaterialTheme.typography.headlineMedium, color = AppTheme.colors.onSurface)
                    ProjectStatusOption.entries.forEach { option ->
                        val current = option.label == project.status
                        ActionRow(localizedStatus(option.label), if (current) AppIcons.check else AppIcons.clock, { if (!current) onChangeStatus(option) },
                            trailing = if (current) stringResource(R.string.language_current) else null, compact = true)
                    }
                    ProjectActionStatus(action, working = stringResource(R.string.ps_salvando))
                }
            }
        }
        if (menuStep == MenuStep.ARCHIVE) AppDialog(
            onDismissRequest = { menuStep = MenuStep.MAIN },
            title = stringResource(R.string.pd_arquivar_este_projeto),
            message = stringResource(R.string.pd_o_projeto_sai_da_sua),
            confirmLabel = stringResource(R.string.pd_arquivar),
            onConfirm = onArchive,
            destructive = true,
            loading = action == ProjectAction.Working,
            content = { ProjectActionStatus(action, working = stringResource(R.string.pd_arquivando)) },
        )
    }
}

/** Foto no topo e painel sobreposto com título, atalhos e ações, como no detalhe carregado. */
@Composable
private fun ProjectDetailSkeleton() {
    Column(Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        SkeletonGroup(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Skeleton(Modifier.fillMaxWidth().height(Sizing.coverHeight), RoundedCornerShape(0.dp))
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
