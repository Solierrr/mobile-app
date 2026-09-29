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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.solaria_mobile.R
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import com.project.solaria_mobile.presentation.navigation.AppDestination
import kotlinx.coroutines.launch

@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
) {
    var activeSheet by remember { mutableStateOf<ProjectSheet?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SolariaWhite)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(345.dp)) {
            Image(
                painter = painterResource(R.drawable.project_building),
                contentDescription = "Fortech Enterprise",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SolariaSpace.screen)
                    .padding(top = SolariaSpace.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack)
                RoundIconButton(LucideR.drawable.lucide_ic_ellipsis, "Mais opções", { menuOpen = true })
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -SolariaSpace.xxl)
                .clip(RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet))
                .background(SolariaWhite)
                .padding(horizontal = SolariaSpace.screen)
                .padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
            verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm),
        ) {
            Text("São Paulo", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Text("Fortech Enterprise", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(SolariaSpace.xs))
            ActionRow("Informações do local", LucideR.drawable.lucide_ic_map_pin, { activeSheet = ProjectSheet.LOCATION })
            ActionRow("Imagens do local", LucideR.drawable.lucide_ic_images, { activeSheet = ProjectSheet.GALLERY })
            ActionRow("Agenda do projeto", LucideR.drawable.lucide_ic_calendar_days, { activeSheet = ProjectSheet.AGENDA }, badge = "10")
            ActionRow("Histórico", LucideR.drawable.lucide_ic_clock, { activeSheet = ProjectSheet.HISTORY })
            ActionRow("Envolvidos", LucideR.drawable.lucide_ic_building, { activeSheet = ProjectSheet.TEAM })
            ActionRow("Conversas", LucideR.drawable.lucide_ic_message_circle, { activeSheet = ProjectSheet.CONVERSATIONS }, badge = "10")
            ActionRow("Empresa responsável", LucideR.drawable.lucide_ic_building, { onNavigate(AppDestination.Enterprise) })
            ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, { onNavigate(AppDestination.Settings) })
        }
    }

    val sheet = activeSheet
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            ProjectSheetContent(
                sheet = sheet,
                onBack = { scope.launch { sheetState.hide(); activeSheet = null } },
                onNavigate = { route -> scope.launch { sheetState.hide(); activeSheet = null; onNavigate(route) } },
            )
        }
    }

    if (menuOpen) {
        ModalBottomSheet(
            onDismissRequest = { menuOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom),
                verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
            ) {
                ScreenTitle("Opções", onBack = { menuOpen = false })
                ActionRow("Listar projetos", LucideR.drawable.lucide_ic_folder, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.Projects) } }, trailing = "24 projetos")
                ActionRow("Novo projeto", LucideR.drawable.lucide_ic_folder_plus, { menuOpen = false })
                ActionRow("Configurações", LucideR.drawable.lucide_ic_settings, { scope.launch { sheetState.hide(); menuOpen = false; onNavigate(AppDestination.Settings) } })
            }
        }
    }
}
