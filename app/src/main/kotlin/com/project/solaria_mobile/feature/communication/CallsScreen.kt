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
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.components.UiStateHost

@Composable
fun CallsRoute(
    onBack: () -> Unit,
    onOpenCall: (String) -> Unit,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Chamados", onBack) }, unavailableMessage = "Seus chamados serão exibidos aqui quando este recurso estiver disponível.") { content -> CallsScreen(content, viewModel::onQueryChange, onBack, onOpenCall) }
}

@Composable
fun CallsScreen(
    state: CallsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCall: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle("Chamados", onBack)
        SearchField("Buscar chamado", state.query, onQueryChange)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            item { SectionHeading("Recentes", faded = true) }
            items(state.calls) { call ->
                CallCard(call, onClick = { onOpenCall(call.id) })
            }
        }
    }
}
