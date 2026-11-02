@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.designsystem.components.SearchField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.PaddingValues
import com.project.solaria_mobile.core.designsystem.components.AppCard
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
import androidx.compose.foundation.layout.Box
import com.project.solaria_mobile.core.designsystem.components.Appear
import com.project.solaria_mobile.core.designsystem.components.AppMenuItem
import com.project.solaria_mobile.core.designsystem.components.AppMenu

@Composable
fun ProjectListRoute(
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenMap: () -> Unit,
    onCreateProject: () -> Unit,
    onOpenCompany: (String) -> Unit = {},
    viewModel: ProjectListViewModel = hiltViewModel(),
    company: OwnCompanyViewModel = hiltViewModel(),
) {
    val companyId by company.companyId.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridMode by viewModel.gridMode.collectAsStateWithLifecycle()
    val layoutFailure by viewModel.layoutFailure.collectAsStateWithLifecycle()
    val invitations by viewModel.invitations.collectAsStateWithLifecycle()
    val invitationAction by viewModel.invitationAction.collectAsStateWithLifecycle()
    val loaded = state as? UiState.Content
    var pulling by remember { mutableStateOf(false) }
    LaunchedEffect(loaded?.refreshing) { if (loaded?.refreshing != true) pulling = false }
    PullToRefreshBox(isRefreshing = pulling && loaded?.refreshing == true, onRefresh = { pulling = true; viewModel.load() }) {
        UiStateHost(state, viewModel::load, loading = { ProjectListSkeleton() }, onBack = onBack, header = { ScreenTitle(stringResource(R.string.pl_projetos), onBack = null) }) { projects ->
            ProjectListScreen(projects, onBack, onOpenProject, onOpenMap, onCreateProject, loaded, viewModel::load,
                gridMode, viewModel::toggleLayout, layoutFailure, invitations, invitationAction, viewModel::respond, companyId, onOpenCompany)
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
    companyId: String? = null,
    onOpenCompany: (String) -> Unit = {},
) {
    val offline = com.project.solaria_mobile.core.offline.LocalOffline.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ProjectFilter.ALL) }
    val visible = filterProjects(projects, query, filter)
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (gridMode) 2 else 1),
        modifier = Modifier.fillMaxSize().appBackground(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.screenTop, bottom = Spacing.screenTop + com.project.solaria_mobile.core.designsystem.theme.LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ScreenTitle(stringResource(R.string.pl_projetos), onBack = null)
                // No topo, para continuar ao alcance com muitos projetos na lista.
                ActionRow(stringResource(R.string.pl_criar_projeto), AppIcons.folder_plus, onClick = onCreateProject)
                companyId?.let { id -> ActionRow(stringResource(R.string.pl_minha_empresa), AppIcons.building, onClick = { onOpenCompany(id) }) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    SectionHeading(stringResource(R.string.pl_seus_projetos), Modifier.weight(1f), faded = true)
                    RefreshButton(onRefresh, resource?.refreshing == true, stringResource(R.string.pl_atualizar_projetos))
                    // Sem projetos não há o que reorganizar.
                    if (projects.isNotEmpty()) RoundIconButton(if (gridMode) AppIcons.layout_list else AppIcons.layout_grid,
                        if (gridMode) stringResource(R.string.pl_mostrar_lista_compacta) else stringResource(R.string.ps_mostrar_grade), onToggleLayout)
                    RoundIconButton(AppIcons.map_pin, stringResource(R.string.ms_mapa_dos_projetos), onOpenMap)
                }
                if (projects.isNotEmpty()) {
                    var filterOpen by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        SearchField(stringResource(R.string.pl_buscar_por_nome_ou_local), query, { query = it }, Modifier.weight(1f))
                        Box {
                            // O botão não muda de cor com o filtro; o filtro escolhido aparece como chip logo abaixo.
                            RoundIconButton(AppIcons.sliders_horizontal, stringResource(R.string.pl_filtrar_projetos), { filterOpen = true })
                            AppMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                                ProjectFilter.entries.forEach { option ->
                                    AppMenuItem(stringResource(option.label), { filter = option; filterOpen = false }, icon = option.icon, selected = option == filter)
                                }
                            }
                        }
                    }
                    if (filter != ProjectFilter.ALL) {
                        Appear { FilterChip(stringResource(filter.label)) { filter = ProjectFilter.ALL } }
                    }
                }
            }
        }
        if (invitations.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeading(stringResource(R.string.pl_convites_recebidos), faded = true)
                invitations.forEach { invitation -> InvitationCard(invitation, invitationAction != ProjectAction.Working, onRespondInvitation) }
                ProjectActionStatus(invitationAction, working = stringResource(R.string.pl_respondendo))
            }
        }
        if (layoutFailure) item(span = { GridItemSpan(maxLineSpan) }) { Text(stringResource(R.string.pl_nao_foi_possivel_salvar_a), color = AppTheme.colors.danger) }
        if (resource?.refreshError != null && !offline) item(span = { GridItemSpan(maxLineSpan) }) { ResourceStatus(resource) }
        if (projects.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            // O aviso ocupa o espaço abaixo do cabeçalho e fica no centro dele, em vez de encostar nos botões do topo.
            Box(Modifier.fillMaxWidth().height((androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.5f).dp), contentAlignment = Alignment.Center) {
                StateNotice(stringResource(R.string.pl_nenhum_projeto_ainda), "Crie um projeto para organizar as atividades, os envolvidos e as informações do local.", icon = AppIcons.folder)
            }
        }
        else if (visible.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { StateNotice(stringResource(R.string.pl_nenhum_projeto_encontrado), "Ajuste a busca ou o filtro para ver outros projetos.", icon = AppIcons.search) }
        items(visible, key = { it.id }) { project -> ProjectListCard(project, gridMode, onClick = { onOpenProject(project.id) }) }
    }
}

/** Cartão de projeto da lista; a tela da empresa reaproveita o mesmo cartão. */
@Composable
internal fun ProjectListCard(project: ProjectSummary, grid: Boolean, onClick: () -> Unit) {
    AppCard(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
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
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(invitation.projectName, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            Text(listOf(invitation.role, invitation.expires).filter { it.isNotBlank() }.joinToString(" · "),
                color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(stringResource(R.string.pl_recusar), { onRespond(invitation.id, false) }, Modifier.weight(1f), enabled = enabled, secondary = true, compact = true)
                PrimaryButton(stringResource(R.string.pl_aceitar), { onRespond(invitation.id, true) }, Modifier.weight(1f), enabled = enabled, compact = true)
            }
        }
    }
}

/** Filtro da lista pela situação: ativos são os abertos e em andamento; encerrados, os concluídos e cancelados. */
enum class ProjectFilter(@androidx.annotation.StringRes val label: Int, @androidx.annotation.DrawableRes val icon: Int, private val statuses: Set<String>?) {
    ALL(R.string.pl_todos, AppIcons.folder, null),
    ACTIVE(R.string.pl_ativos, AppIcons.clock, setOf("Aberto", "Em andamento")),
    CLOSED(R.string.pl_encerrados, AppIcons.lock, setOf("Concluído", "Cancelado")),
    ;

    fun accepts(status: String): Boolean = statuses == null || status in statuses
}

private val ACCENTS = Regex("""\p{Mn}+""")

private fun normalized(text: String): String = ACCENTS.replace(java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFD), "").lowercase()

/** Busca por nome, local ou empresa, sem diferenciar maiúsculas nem acentos, combinada com o filtro de situação. */
internal fun filterProjects(projects: List<ProjectSummary>, query: String, filter: ProjectFilter): List<ProjectSummary> {
    val needle = normalized(query)
    return projects.filter { project ->
        filter.accepts(project.status) &&
            (needle.isEmpty() || listOf(project.name, project.place, project.companyName).any { normalized(it).contains(needle) })
    }
}

/** Filtro ativo, como um chip que o toque remove. */
@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onRemove, shape = androidx.compose.foundation.shape.RoundedCornerShape(Radius.pill), color = AppTheme.colors.brandContainer,
    ) {
        Row(Modifier.padding(start = Spacing.md, end = Spacing.xs).padding(vertical = Spacing.xxs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            androidx.compose.material3.Text(label, color = AppTheme.colors.brandStrong, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(AppIcons.x), stringResource(R.string.pl_remover_filtro), Modifier.size(Spacing.lg), tint = AppTheme.colors.brandStrong)
        }
    }
}
