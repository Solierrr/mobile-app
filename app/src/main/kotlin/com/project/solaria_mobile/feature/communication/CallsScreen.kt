@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
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
import com.project.solaria_mobile.core.designsystem.theme.appBackground
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.components.ListSkeleton
import com.project.solaria_mobile.core.designsystem.components.RefreshButton
import com.project.solaria_mobile.core.designsystem.components.ResourceStatus
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.common.UiState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun CallsRoute(
    onBack: () -> Unit,
    onOpenCall: (String) -> Unit,
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.load() }
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle(stringResource(R.string.hm_chamados), onBack) }, loading = { ListSkeleton(circle = false) }, emptyMessage = stringResource(R.string.cl_nenhum_servico_tecnico_por_enquanto)) { content -> CallsScreen(content, viewModel::onQueryChange, onBack, onOpenCall, viewModel::load, state as? UiState.Content) }
}

@Composable
fun CallsScreen(
    state: CallsUiState,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCall: (String) -> Unit,
    onRefresh: () -> Unit = {},
    resource: UiState.Content<*>? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().appBackground().padding(horizontal = Spacing.screen).padding(top = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle(stringResource(R.string.hm_chamados), onBack, actions = { RefreshButton(onRefresh, resource?.refreshing == true) })
        resource?.let { ResourceStatus(it) }
        SearchField(stringResource(R.string.cl_buscar_chamado), state.query, onQueryChange)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (state.calls.isEmpty()) item { StateNotice(stringResource(R.string.cl_nenhum_servico_encontrado), stringResource(R.string.settings_no_results_description)) }
            item { SectionHeading(stringResource(R.string.cl_recentes), faded = true) }
            items(state.calls) { call ->
                CallCard(call, onClick = { onOpenCall(call.id) })
            }
        }
    }
}
