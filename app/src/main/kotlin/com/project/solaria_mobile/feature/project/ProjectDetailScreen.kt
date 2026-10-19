@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.composables.icons.lucide.R as LucideR
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
    UiStateHost(state, viewModel::load) { project -> ProjectDetailScreen(project, sections, onBack, onNavigate) }
}

@Composable
fun ProjectDetailScreen(
    project: ProjectSummary,
    sections: ProjectSectionsViewModel,
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
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
        Box(modifier = Modifier.fillMaxWidth().height(345.dp)) {
            if (coverId != null) {
                RemoteImage(
                    key = coverId,
                    load = sections::photoContent,
                    description = project.name,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.project_building),
                    contentDescription = project.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen)
                    .padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack)
                RoundIconButton(LucideR.drawable.lucide_ic_ellipsis, "Mais opções", { menuOpen = true })
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
            Text(project.place, color = AppTheme.colors.onSurfaceMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text(project.name, color = AppTheme.colors.onSurface, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Spacing.xs))
            ActionRow("Informações do local", LucideR.drawable.lucide_ic_map_pin, { open(ProjectSheet.LOCATION) })
            ActionRow("Imagens do local", LucideR.drawable.lucide_ic_images, { open(ProjectSheet.GALLERY) })
            ActionRow("Agenda do projeto", LucideR.drawable.lucide_ic_calendar_days, { open(ProjectSheet.AGENDA) })
            ActionRow("Histórico", LucideR.drawable.lucide_ic_clock, { open(ProjectSheet.HISTORY) })
            ActionRow("Envolvidos", LucideR.drawable.lucide_ic_building, { open(ProjectSheet.TEAM) })
            ActionRow("Conversas", LucideR.drawable.lucide_ic_message_circle, { open(ProjectSheet.CONVERSATIONS) })
            ActionRow("Empresa responsável", LucideR.drawable.lucide_ic_building, { onNavigate(AppDestination.Enterprise) })
            ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, { onNavigate(AppDestination.Settings) })
        }
    }

    val sheet = activeSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
            dragHandle = null,
        ) {
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
        ModalBottomSheet(
            onDismissRequest = { menuOpen = false },
            sheetState = sheetState,
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xxl, bottom = Spacing.screenBottom),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ScreenTitle("Opções", onBack = { menuOpen = false })
                ActionRow("Listar projetos", LucideR.drawable.lucide_ic_folder, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.Projects) } }, trailing = "24 projetos")
                ActionRow("Novo projeto", LucideR.drawable.lucide_ic_folder_plus, { menuOpen = false })
                ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.Settings) } })
            }
        }
    }
}
