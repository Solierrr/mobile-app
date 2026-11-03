package com.project.solaria_mobile.feature.communication

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.network.realtime.RealtimeState

/** O histórico continua disponível por REST enquanto o canal de eventos se recupera. */
@Composable
internal fun RealtimeNotice(state: RealtimeState, modifier: Modifier = Modifier) {
    val label = when (state) {
        RealtimeState.Connected -> return
        RealtimeState.Connecting -> R.string.ch_conectando
        is RealtimeState.Reconnecting -> R.string.ch_reconectando
        else -> R.string.ch_sem_tempo_real
    }
    Text(stringResource(label), modifier, color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium)
}
