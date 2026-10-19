package com.project.solaria_mobile.core.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** [offline] = sem internet agora; [stale] = a tela mostra dados guardados; [lastSyncedAt] = quando foram atualizados. */
data class OfflineBannerState(val offline: Boolean = false, val stale: Boolean = false, val lastSyncedAt: Long? = null) {
    val visible: Boolean get() = offline || stale
}

internal fun bannerText(state: OfflineBannerState, now: Long): String {
    val age = state.lastSyncedAt?.let { " Atualizado ${ageLabel(it, now)}." }.orEmpty()
    return if (state.offline) "Sem internet. Mostrando os dados guardados.$age" else "Mostrando os dados guardados.$age"
}

@HiltViewModel
class OfflineViewModel @Inject constructor(network: NetworkMonitor, reader: OfflineReader) : ViewModel() {
    val state: StateFlow<OfflineBannerState> = combine(network.isOnline, reader.status) { online, status ->
        OfflineBannerState(offline = !online, stale = status.stale, lastSyncedAt = status.lastSyncedAt)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OfflineBannerState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Faixa discreta no topo: avisa quando não há internet ou quando a tela mostra dados guardados. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, viewModel: OfflineViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.visible) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.headerFill)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = bannerText(state, System.currentTimeMillis()),
            color = AppTheme.colors.onSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
