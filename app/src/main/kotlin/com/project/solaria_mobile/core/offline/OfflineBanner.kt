package com.project.solaria_mobile.core.offline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class OfflineViewModel @Inject constructor(network: NetworkMonitor) : ViewModel() {
    val offline = network.isOnline.map { !it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

val LocalOffline = staticCompositionLocalOf { false }

@Composable
fun OfflineStatus(modifier: Modifier = Modifier, viewModel: OfflineViewModel = hiltViewModel(), content: @Composable ColumnScope.() -> Unit) {
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalOffline provides offline) {
        Column(modifier) {
            if (offline) Box(Modifier.fillMaxWidth().background(AppTheme.colors.inputFill).padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                Text("Sem internet. Conteúdo já sincronizado continua disponível.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.onSurface)
            }
            content()
        }
    }
}
