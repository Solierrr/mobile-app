@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.project.solaria_mobile.feature.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SegmentedControl
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.userMessage
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Sizing
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import com.project.solaria_mobile.feature.project.domain.hasCoordinates
import com.project.solaria_mobile.feature.project.ProjectCardImage
import com.project.solaria_mobile.feature.project.projectStatusTint
import com.project.solaria_mobile.core.designsystem.components.StatusBadge
import androidx.compose.ui.draw.clip

@Composable
internal fun MapSheetHandle(expanded: Boolean, onToggle: () -> Unit) {
    Surface(onClick = onToggle, color = AppTheme.colors.surface,
        modifier = Modifier.fillMaxWidth().height(Spacing.xxl).semantics {
            contentDescription = "Expandir ou recolher painel"
            stateDescription = if (expanded) "Painel expandido" else "Painel recolhido"
        }) {
        Box(contentAlignment = Alignment.Center) {
            Surface(Modifier.size(width = 64.dp, height = 4.dp), shape = RoundedCornerShape(Radius.pill), color = AppTheme.colors.outline) { }
        }
    }
}

@Composable
internal fun MapProjectPanel(state: MapUiState, resource: UiState.Content<*>?, selected: ProjectSummary?, saved: Set<String>,
    onSelect: (String) -> Unit, onSave: (String) -> Unit, onResolve: () -> Unit, onClose: () -> Unit,
    onOpen: (String) -> Unit, onDirections: (ProjectSummary) -> Unit, onExpand: () -> Unit,
    expanded: Boolean, onTogglePanel: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var savedOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = state.projects.filter { (!savedOnly || it.id in saved) &&
        "${it.name} ${it.address} ${it.companyName}".contains(query.trim(), ignoreCase = true) }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .6f).dp),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(selected?.name ?: "Seus projetos", style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(selected?.address?.ifBlank { "Endereço não informado" } ?: if (savedOnly) "Locais salvos neste aparelho" else "Seus locais, em um só lugar.",
                        style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (selected != null) RoundIconButton(AppIcons.chevron_left, "Voltar aos projetos", onClose)
                else RoundIconButton(AppIcons.chevron_up, if (expanded) "Recolher painel" else "Abrir painel", onTogglePanel,
                    Modifier.rotate(if (expanded) 180f else 0f))
            }
        }
        resource?.let { item { ResourceStatus(it) } }
        if (selected != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ProjectCardImage(selected.id, Modifier.fillMaxWidth().height(PANEL_PHOTO_HEIGHT).clip(RoundedCornerShape(Radius.large)))
                    StatusBadge(selected.status, tint = projectStatusTint(selected.status))
                    if (selected.companyName.isNotBlank()) Text(selected.companyName, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Abrir projeto", { onOpen(selected.id) }, Modifier.weight(1f), compact = true, gradient = true)
                    RoundIconButton(AppIcons.bookmark, if (selected.id in saved) "Remover dos salvos" else "Salvar local", { onSave(selected.id) },
                        Modifier.semantics { stateDescription = if (selected.id in saved) "Salvo neste aparelho" else "Não salvo" },
                        background = if (selected.id in saved) AppTheme.colors.outline else AppTheme.colors.inputFill)
                }
            }
            if (selected.address.isNotBlank()) item {
                TextButton(onClick = { onDirections(selected) }, modifier = Modifier.fillMaxWidth().heightIn(min = Sizing.touchTarget)) {
                    Text("Como chegar", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
                }
            }
            if (!selected.hasCoordinates() && selected.address.isNotBlank()) item { LocateProjects(state, resource, onResolve) }
        } else {
            item { SearchField("Buscar projeto ou endereço", query, { query = it; onExpand() }) }
            item { SegmentedControl(listOf("Projetos", "Salvos"), if (savedOnly) 1 else 0, { savedOnly = it == 1; onExpand() }) }
            if (state.projects.isEmpty()) item {
                StateNotice("Nenhum projeto por enquanto", "Os projetos aos quais você tem acesso aparecerão aqui.", icon = AppIcons.map_pin)
            } else if (filtered.isEmpty()) item {
                StateNotice(if (query.isNotBlank()) "Nenhum projeto encontrado" else "Seus locais ficam aqui",
                    if (query.isNotBlank()) "Tente outro nome ou endereço." else "Salve o local pelo resumo de um projeto.", icon = AppIcons.bookmark)
            }
            items(filtered, key = { it.id }) { project ->
                ActionRow(project.name, AppIcons.map_pin, { onSelect(project.id) },
                    subtitle = listOf(project.companyName, project.address.ifBlank { "Endereço não informado" }).filter { it.isNotBlank() }.joinToString("\n"), compact = true)
            }
            if (state.projects.any { !it.hasCoordinates() && it.address.isNotBlank() }) item { LocateProjects(state, resource, onResolve) }
        }
        state.locationError?.let { error -> item { Text(error.userMessage(), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.danger) } }
    }
}

@Composable
private fun LocateProjects(state: MapUiState, resource: UiState.Content<*>?, onResolve: () -> Unit) {
    PrimaryButton(if (state.resolving) "Localizando projetos…" else "Localizar projetos no mapa", onResolve,
        enabled = !state.resolving && resource?.cached != true, secondary = true, compact = true)
}

private val PANEL_PHOTO_HEIGHT = 160.dp
