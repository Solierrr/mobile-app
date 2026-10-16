package com.project.solaria_mobile.feature.diagnostics

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.components.UiStateHost
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsRoute(onBack: () -> Unit, viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateHost(state, viewModel::load) { content -> DiagnosticsScreen(content, onBack, viewModel::load) }
}

@Composable
fun DiagnosticsScreen(state: DiagnosticsUiState, onBack: () -> Unit, onRefresh: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ScreenTitle("Diagnóstico", onBack)
        SectionHeading("Ambiente", faded = true)
        DiagnosticsRow("Ambiente", state.environment)
        DiagnosticsRow("api-auth", state.authUrl)
        DiagnosticsRow("api-core", state.coreUrl)
        DiagnosticsRow("api-messenger", state.messengerUrl)
        SectionHeading("Instalação", faded = true)
        DiagnosticsRow("FID", state.installationId ?: "indisponível (sem google-services.json)")
        state.installationId?.let { id ->
            TextButton(onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("fid", id))) } }) {
                Text("Copiar FID", color = AppTheme.colors.primary)
            }
        }
        SectionHeading("google-registry", faded = true)
        DiagnosticsRow("Endereço", state.registryUrl)
        DiagnosticsRow("Saúde", state.registryStatus)
        DiagnosticsRow("Geocodificação", state.geocodeSample)
        TextButton(onClick = onRefresh) { Text("Atualizar", color = AppTheme.colors.primary) }
    }
}

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(label, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}
