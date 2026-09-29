@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.MapBackdrop
import com.project.solaria_mobile.core.designsystem.components.RoundIconButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.theme.SolariaInk
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaRadius
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaSize
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite
import kotlinx.coroutines.launch

@Composable
fun MapRoute(onBack: () -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapScreen(state, viewModel::toggleSaved, onBack)
}

@Composable
fun MapScreen(state: MapUiState, onToggleSaved: () -> Unit, onBack: () -> Unit) {
    var locationsOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(SolariaWhite)) {
        MapBackdrop(modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RoundIconButton(LucideR.drawable.lucide_ic_chevron_left, "Voltar", onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                RoundIconButton(LucideR.drawable.lucide_ic_search, "Buscar local", {})
                RoundIconButton(LucideR.drawable.lucide_ic_ellipsis, "Locais salvos", { locationsOpen = true })
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(bottom = SolariaSpace.screenBottom),
            shape = RoundedCornerShape(SolariaRadius.sheet),
            color = SolariaWhite,
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(SolariaSpace.xl), verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(LucideR.drawable.lucide_ic_chevron_up, "Expandir detalhes", { locationsOpen = true }, size = SolariaSize.compactMapControl)
                    Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                        RoundIconButton(LucideR.drawable.lucide_ic_map_pin, "Minha localização", {}, size = SolariaSize.compactMapControl)
                        RoundIconButton(LucideR.drawable.lucide_ic_maximize, "Expandir mapa", {}, size = SolariaSize.compactMapControl)
                    }
                }
                Text("Há 22 km da localização atual", color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text("Santana de Parnaíba, SP", color = SolariaInk, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                    ActionRow("Google Maps", LucideR.drawable.lucide_ic_map_pin, {}, compact = true, modifier = Modifier.weight(1f))
                    ActionRow(if (state.saved) "Salvo" else "Salvar", LucideR.drawable.lucide_ic_bookmark, onToggleSaved, compact = true, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (locationsOpen) {
        ModalBottomSheet(
            onDismissRequest = { locationsOpen = false },
            sheetState = sheetState,
            containerColor = SolariaWhite,
            shape = RoundedCornerShape(topStart = SolariaRadius.sheet, topEnd = SolariaRadius.sheet),
            dragHandle = null,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.xxl, bottom = SolariaSpace.screenBottom), verticalArrangement = Arrangement.spacedBy(SolariaSpace.md)) {
                ScreenTitle("Locais salvos", onBack = { scope.launch { sheetState.hide(); locationsOpen = false } })
                LazyColumn(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
                    items(state.savedLocations) { location ->
                        ActionRow(location, LucideR.drawable.lucide_ic_map_pin, { locationsOpen = false }, trailing = "···")
                    }
                }
            }
        }
    }
}
