package com.project.solaria_mobile.feature.discovery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.UiStateHost

@Composable
fun MapRoute(onBack: () -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load, onBack = onBack, header = { ScreenTitle("Mapa", onBack) }, unavailableMessage = "Por enquanto, consulte o endereço nas informações de cada projeto.") {
        StateNotice("O mapa está em preparação", "Consulte o endereço nas informações de cada projeto.")
    }
}
