package com.project.solaria_mobile.core.designsystem.components

import com.project.solaria_mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.offline.LocalOffline

@Composable
fun ResourceStatus(state: UiState.Content<*>) {
    if (LocalOffline.current || state.refreshError == null) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(stringResource(R.string.rs_voce_esta_vendo_os_dados), style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
        Text(stringResource(R.string.rs_nao_conseguimos_atualizar_agora_voce), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
    }
}
