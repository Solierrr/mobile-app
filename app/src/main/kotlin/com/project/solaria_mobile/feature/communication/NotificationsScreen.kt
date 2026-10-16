@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.communication

import com.project.solaria_mobile.feature.communication.data.ServiceCall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.SolariaInkMuted
import com.project.solaria_mobile.core.designsystem.theme.SolariaSpace
import com.project.solaria_mobile.core.designsystem.theme.SolariaWhite

@Composable
fun NotificationsRoute(onBack: () -> Unit, viewModel: NotificationsViewModel = hiltViewModel()) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    NotificationsScreen(groups, onBack)
}

@Composable
fun NotificationsScreen(groups: List<Pair<String, List<ServiceCall>>>, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SolariaWhite).padding(horizontal = SolariaSpace.screen).padding(top = SolariaSpace.lg)) {
        ScreenTitle("Notificações", onBack, trailingIcon = LucideR.drawable.lucide_ic_bell_dot, trailingLabel = "Marcar tudo como lido", onTrailingClick = {})
        LazyColumn(modifier = Modifier.padding(top = SolariaSpace.md), verticalArrangement = Arrangement.spacedBy(SolariaSpace.sm)) {
            groups.forEach { (title, notifications) ->
                item { SectionHeading(title, faded = true) }
                items(notifications) { call -> CallCard(call, onClick = {}) }
            }
            item {
                Text("Você está em dia com suas notificações.", modifier = Modifier.fillMaxWidth().padding(SolariaSpace.xl), color = SolariaInkMuted, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
