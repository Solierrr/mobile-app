@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.core.designsystem.components.AppButton
import com.project.solaria_mobile.core.designsystem.components.ButtonVariant
import com.project.solaria_mobile.core.designsystem.components.ButtonSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    val handleDescription = stringResource(R.string.mp_expandir_ou_recolher_painel)
    val handleState = stringResource(if (expanded) R.string.mp_painel_expandido else R.string.mp_painel_recolhido)
    Surface(onClick = onToggle, color = AppTheme.colors.surface,
        modifier = Modifier.fillMaxWidth().height(Spacing.xxl).semantics {
            contentDescription = handleDescription
            stateDescription = handleState
        }) {
        Box(contentAlignment = Alignment.Center) {
            Surface(Modifier.size(width = Sizing.sheetHandleWidth, height = Sizing.sheetHandleHeight), shape = RoundedCornerShape(Radius.pill), color = AppTheme.colors.outline) { }
        }
    }
}

@Composable
internal fun MapProjectPanel(state: MapUiState, resource: UiState.Content<*>?, selected: ProjectSummary?, saved: Set<String>,
    onSelect: (String) -> Unit, onSave: (String) -> Unit, onClose: () -> Unit,
    onOpen: (String) -> Unit, onDirections: (ProjectSummary) -> Unit, onExpand: () -> Unit,
    expanded: Boolean, onTogglePanel: () -> Unit, onAssistant: (ProjectSummary) -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var savedOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = state.projects.filter { (!savedOnly || it.id in saved) &&
        "${it.name} ${it.address} ${it.companyName}".contains(query.trim(), ignoreCase = true) }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .6f).dp),
        contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text(selected?.name ?: stringResource(R.string.pl_seus_projetos), style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.onSurface,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(selected?.address?.ifBlank { stringResource(R.string.onboarding_review_no_address) } ?: if (savedOnly) stringResource(R.string.mp_locais_salvos_neste_aparelho) else stringResource(R.string.mp_seus_locais_em_um_so),
                        style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (selected != null) RoundIconButton(AppIcons.x, stringResource(R.string.mp_fechar_projeto_e_voltar_a), onClose)
                else RoundIconButton(AppIcons.chevron_up, if (expanded) stringResource(R.string.mp_recolher_painel) else stringResource(R.string.mp_abrir_painel), onTogglePanel,
                    Modifier.rotate(if (expanded) 180f else 0f))
            }
            if (selected != null) {
                // Fazem parte do bloco de texto: vários chips cabem e a fileira rola para o lado.
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(selected.status, tint = projectStatusTint(selected.status))
                    StatusBadge(selected.companyName)
                }
            }
            }
        }
        resource?.let { item { ResourceStatus(it) } }
        if (selected != null) {
            item {
                ProjectCardImage(selected.id, Modifier.fillMaxWidth().padding(top = Spacing.xs).height(PANEL_PHOTO_HEIGHT).clip(RoundedCornerShape(Radius.large)))
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    AppButton(stringResource(R.string.mp_abrir_projeto), { onOpen(selected.id) }, Modifier.weight(1f), variant = ButtonVariant.Secondary, size = ButtonSize.Medium, leadingIcon = AppIcons.folder)
                    val savedState = stringResource(if (selected.id in saved) R.string.mp_salvo_neste_aparelho else R.string.mp_nao_salvo)
                    // O assistente geográfico fica à esquerda do favorito; os dois seguem o mesmo cinza dos outros botões.
                    RoundIconButton(AppIcons.sparkles, stringResource(R.string.geo_assistente), { onAssistant(selected) })
                    // Salvo troca o ícone para o preenchido, sem mudar a cor do botão.
                    RoundIconButton(if (selected.id in saved) AppIcons.bookmark_filled else AppIcons.bookmark,
                        if (selected.id in saved) stringResource(R.string.mp_remover_dos_salvos) else stringResource(R.string.mp_salvar_local), { onSave(selected.id) },
                        Modifier.semantics { stateDescription = savedState })
                }
            }
            if (selected.address.isNotBlank()) item {
                AppButton(stringResource(R.string.mp_tracar_rota), { onDirections(selected) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, size = ButtonSize.Medium, leadingIcon = AppIcons.navigation)
            }
        } else {
            item { SearchField(stringResource(R.string.mp_buscar_projeto_ou_endereco), query, { query = it; onExpand() }) }
            item { SegmentedControl(listOf(stringResource(R.string.pl_projetos), stringResource(R.string.mp_salvos)), if (savedOnly) 1 else 0, { savedOnly = it == 1; onExpand() }) }
            if (state.projects.isEmpty()) item {
                StateNotice(stringResource(R.string.mp_nenhum_projeto_por_enquanto), "Os projetos aos quais você tem acesso aparecerão aqui.", icon = AppIcons.map_pin)
            } else if (filtered.isEmpty()) item {
                StateNotice(if (query.isNotBlank()) stringResource(R.string.pl_nenhum_projeto_encontrado) else stringResource(R.string.mp_seus_locais_ficam_aqui),
                    if (query.isNotBlank()) stringResource(R.string.mp_tente_outro_nome_ou_endereco) else stringResource(R.string.mp_salve_o_local_pelo_resumo), icon = AppIcons.bookmark)
            }
            items(filtered, key = { it.id }) { project ->
                ActionRow(project.name, AppIcons.map_pin, { onSelect(project.id) },
                    subtitle = listOf(project.companyName, project.address.ifBlank { stringResource(R.string.onboarding_review_no_address) }).filter { it.isNotBlank() }.joinToString("\n"), compact = true)
            }
        }
        state.locationError?.let { error -> item { Text(error.userMessage(), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.danger) } }
    }
}

private val PANEL_PHOTO_HEIGHT = 160.dp
