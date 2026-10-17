@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.AppTheme

@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenDiagnostics: () -> Unit, onSignOut: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, viewModel::onQueryChange, onBack, onOpenProfile, onOpenDiagnostics, onSignOut)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onQueryChange: (String) -> Unit, onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenDiagnostics: () -> Unit, onSignOut: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppTheme.colors.surface).padding(horizontal = Spacing.screen).padding(top = Spacing.lg).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        ScreenTitle("Configurações", onBack, trailingIcon = LucideR.drawable.lucide_ic_circle_question_mark, trailingLabel = "Ajuda", onTrailingClick = {})
        SearchField("Procure o que quiser", state.query, onQueryChange, trailingIcon = LucideR.drawable.lucide_ic_sliders_horizontal, onTrailingClick = {})
        state.groups.forEach { group ->
            SectionHeading(group.title, faded = true)
            group.actions.forEach { action -> ActionRow(action.title, action.icon, {
                when (action.target) {
                    SettingTarget.PROFILE -> onOpenProfile()
                    SettingTarget.DIAGNOSTICS -> onOpenDiagnostics()
                    SettingTarget.SIGN_OUT -> onSignOut()
                    SettingTarget.NONE -> Unit
                }
            }) }
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}
