@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun CallsRoute(
    onBack: () -> Unit,
    onOpenCall: () -> Unit,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CallsScreen(state, viewModel::onQueryChange, onBack, onOpenCall)
}

@Composable
fun CallsScreen(
    state: CallsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCall: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg),
        verticalArrangement = Arrangement.spacedBy(SolariaSpace.md),
    ) {
        ScreenTitle("Chamados", onBack, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, trailingLabel = "Filtrar", onTrailingClick = {})
        SearchField("Buscar chamado", state.query, onQueryChange, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        LazyColumn(verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
            item { SectionHeading("Recentes", faded = true) }
            items(state.calls) { call ->
                CallCard(call, onClick = onOpenCall)
            }
        }
    }
}
