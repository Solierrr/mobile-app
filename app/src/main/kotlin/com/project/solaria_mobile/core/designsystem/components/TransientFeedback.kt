package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Elevation
import com.project.solaria_mobile.core.designsystem.theme.Radius
import com.project.solaria_mobile.core.designsystem.theme.Spacing

@Composable
fun TransientFeedback(message: String?, onDismiss: (String) -> Unit, modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(message) {
        if (message != null) {
            host.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short)
            dismiss(message)
        }
    }
    SnackbarHost(host, modifier) { data ->
        Surface(shape = RoundedCornerShape(Radius.large), color = AppTheme.colors.surface, shadowElevation = Elevation.floating) {
            Row(Modifier.padding(Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(data.visuals.message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurface)
                RoundIconButton(AppIcons.x, "Fechar aviso", data::dismiss)
            }
        }
    }
}
