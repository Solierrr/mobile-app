@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.project.solaria_mobile.BuildConfig
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.core.designsystem.components.ActionRow
import com.project.solaria_mobile.core.designsystem.components.StateNotice
import com.project.solaria_mobile.core.designsystem.components.PrimaryButton
import com.project.solaria_mobile.core.designsystem.components.ScreenTitle
import com.project.solaria_mobile.core.designsystem.components.SearchField
import com.project.solaria_mobile.core.designsystem.components.SectionHeading
import com.project.solaria_mobile.core.designsystem.theme.Spacing
import com.project.solaria_mobile.core.designsystem.theme.AppTheme
import com.project.solaria_mobile.core.designsystem.theme.Radius

@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenDiagnostics: () -> Unit, onSignOut: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, viewModel::onQueryChange, viewModel::onMockModeChange, onBack, onOpenProfile, onOpenDiagnostics, onSignOut)
}

@Composable
fun SettingsScreen(state: SettingsUiState, onQueryChange: (String) -> Unit, onMockModeChange: (Boolean) -> Unit, onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenDiagnostics: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmSignOut by remember { mutableStateOf(false) }
    fun openLink(url: String, email: Boolean = false) {
        val intent = Intent(if (email) Intent.ACTION_SENDTO else Intent.ACTION_VIEW, url.toUri())
        runCatching { context.startActivity(intent) }.onFailure {
            scope.launch { snackbar.showSnackbar(if (email) "Escreva para solaria.innetwork@gmail.com." else "Não foi possível abrir o navegador.") }
        }
    }
    Box(Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(top = Spacing.screenTop, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScreenTitle("Configurações", onBack)
                SearchField("Buscar nas configurações", state.query, onQueryChange)
            }
            if (state.groups.isEmpty()) StateNotice("Nenhuma opção encontrada", "Tente outro termo ou limpe a busca.", icon = AppIcons.search, actions = { PrimaryButton("Limpar busca", { onQueryChange("") }, secondary = true, compact = true) })
            state.groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionHeading(group.title)
                    if (group.title == "Desenvolvimento" && (state.query.isBlank() || "Modo mock".contains(state.query, true))) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.medium))
                                .background(AppTheme.colors.inputFill)
                                .toggleable(state.mockModeEnabled, role = Role.Switch, onValueChange = onMockModeChange)
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                                Text("Modo mock", color = AppTheme.colors.onSurface, style = MaterialTheme.typography.bodyLarge)
                                Text("Usar dados locais sem acessar os serviços de QA.", color = AppTheme.colors.onSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = state.mockModeEnabled, onCheckedChange = null)
                        }
                    }
                    group.actions.forEach { action -> ActionRow(action.title, action.icon, {
                        when (action.target) {
                            SettingTarget.PROFILE -> onOpenProfile()
                            SettingTarget.DIAGNOSTICS -> onOpenDiagnostics()
                            SettingTarget.SIGN_OUT -> confirmSignOut = true
                            SettingTarget.SUPPORT -> openLink("mailto:solaria.innetwork@gmail.com", true)
                            SettingTarget.PRIVACY -> openLink("https://solarianetwork.site/pt-BR/politicas/privacidade")
                            SettingTarget.TERMS -> openLink("https://solarianetwork.site/pt-BR/politicas/termos")
                        }
                    }) }
                }
            }
            Text("Solaria · Versão ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (confirmSignOut) AlertDialog(
        onDismissRequest = { confirmSignOut = false },
        title = { Text("Sair da sua conta?", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("Você precisará entrar novamente para acessar seus projetos. Os dados locais desta sessão serão removidos.", style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { TextButton(onClick = { confirmSignOut = false; onSignOut() }) { Text("Sair da conta") } },
        dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Continuar no aplicativo") } },
        containerColor = AppTheme.colors.surface,
    )
}
