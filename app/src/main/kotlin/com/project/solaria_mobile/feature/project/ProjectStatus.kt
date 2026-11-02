package com.project.solaria_mobile.feature.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

/** Cor do selo de status do projeto: laranja só para o que está em andamento; o texto continua dizendo o status. */
@Composable
internal fun projectStatusTint(status: String): Color = when (status) {
    "Concluído" -> AppTheme.colors.success
    "Cancelado" -> AppTheme.colors.danger
    "Em andamento" -> AppTheme.colors.brandStrong
    else -> AppTheme.colors.onSurfaceMuted
}
