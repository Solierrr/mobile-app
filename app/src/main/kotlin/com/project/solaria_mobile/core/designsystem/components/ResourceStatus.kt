package com.project.solaria_mobile.core.designsystem.components

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
        Text("Você está vendo os dados salvos", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.onSurface)
        Text("Não conseguimos atualizar agora. Você pode continuar consultando este conteúdo.", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
    }
}
