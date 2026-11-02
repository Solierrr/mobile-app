@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.project.solaria_mobile.feature.profile

import com.project.solaria_mobile.core.designsystem.components.AppToastHost
import com.project.solaria_mobile.core.designsystem.components.ToastKind
import com.project.solaria_mobile.core.designsystem.components.rememberToastState
import com.project.solaria_mobile.core.designsystem.components.AppSwitchRow
import com.project.solaria_mobile.core.designsystem.components.AppDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.designsystem.components.BottomSheet
import com.project.solaria_mobile.core.designsystem.components.LocalSheetDismiss
import com.project.solaria_mobile.core.designsystem.components.SheetSize
import com.project.solaria_mobile.core.locale.AppLanguage
import com.project.solaria_mobile.core.locale.AppLocale
import com.project.solaria_mobile.core.locale.findActivity
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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

@Composable
fun SettingsRoute(onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenDiagnostics: () -> Unit, onSignOut: () -> Unit, onOnboardingRestarted: () -> Unit = {}, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(state, viewModel::onQueryChange, viewModel::onMockModeChange, onBack, onOpenProfile, onOpenDiagnostics, onSignOut, viewModel::onMockImagesChange, viewModel::simulateIncomingMessage, onMockEmptyAccountChange = viewModel::onMockEmptyAccountChange) {
        viewModel.restartOnboarding().also { if (it) onOnboardingRestarted() }
    }
}

@Composable
fun SettingsScreen(state: SettingsUiState, onQueryChange: (String) -> Unit, onMockModeChange: (Boolean) -> Unit, onBack: () -> Unit, onOpenProfile: () -> Unit, onOpenDiagnostics: () -> Unit, onSignOut: () -> Unit, onMockImagesChange: (Boolean) -> Unit = {}, onSimulateMessage: () -> Unit = {}, onMockEmptyAccountChange: (Boolean) -> Unit = {}, onRestartOnboarding: () -> Boolean = { false }) {
    val context = LocalContext.current
    val resources = context.resources
    val toast = rememberToastState()
    val scope = rememberCoroutineScope()
    var confirmSignOut by remember { mutableStateOf(false) }
    var choosingLanguage by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(AppLocale.saved(context)) }
    val mailFallback = stringResource(R.string.settings_mail_fallback)
    val mockOnly = stringResource(R.string.st_so_no_modo_mock)
    val browserFailed = stringResource(R.string.settings_browser_failed)
    fun openLink(url: String, email: Boolean = false) {
        val intent = Intent(if (email) Intent.ACTION_SENDTO else Intent.ACTION_VIEW, url.toUri())
        runCatching { context.startActivity(intent) }.onFailure {
            scope.launch { if (email) toast.show(mailFallback, ToastKind.Info) else toast.show(browserFailed, ToastKind.Error) }
        }
    }
    // A busca compara com o texto no idioma atual: quem usa o app em inglês procura "Privacy", não "Privacidade".
    val mockMatches = state.query.isBlank() || "Modo mock".contains(state.query, true)
    val groups = state.groups.map { group ->
        group.copy(actions = group.actions.filter { resources.getString(it.title).contains(state.query, ignoreCase = true) })
    }.filter { group -> group.actions.isNotEmpty() || (group.development && mockMatches) }
    Box(Modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(top = Spacing.screenTop, bottom = Spacing.screenBottom), verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ScreenTitle(stringResource(R.string.settings_title), onBack)
                SearchField(stringResource(R.string.settings_search), state.query, onQueryChange)
            }
            if (groups.isEmpty()) StateNotice(stringResource(R.string.settings_no_results_title), stringResource(R.string.settings_no_results_description), icon = AppIcons.search, actions = { PrimaryButton(stringResource(R.string.settings_clear_search), { onQueryChange("") }, secondary = true, compact = true) })
            groups.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SectionHeading(stringResource(group.title))
                    if (group.development && mockMatches) {
                        AppSwitchRow("Modo mock", state.mockModeEnabled, onMockModeChange, description = "Usar dados locais sem acessar os serviços de QA.")
                        if (state.mockModeEnabled) {
                            AppSwitchRow("Imagens de demonstração", state.mockImagesEnabled, onMockImagesChange, description = "Fotos de exemplo nas galerias dos projetos. Desligue para testar o estado sem imagens.")
                            AppSwitchRow("Conta sem projetos", state.mockEmptyAccount, onMockEmptyAccountChange, description = "Simula uma conta recém-criada, sem projetos, chamados, conversas, conexões nem notificações. Fica ligada até você desligar.")
                        }
                    }
                    group.actions.forEach { action -> ActionRow(stringResource(action.title), action.icon, {
                        when (action.target) {
                            SettingTarget.PROFILE -> onOpenProfile()
                            SettingTarget.LANGUAGE -> choosingLanguage = true
                            SettingTarget.DIAGNOSTICS -> onOpenDiagnostics()
                            SettingTarget.SIGN_OUT -> confirmSignOut = true
                            SettingTarget.SIMULATE_DEEPLINK -> onSignOut()
                            SettingTarget.SIMULATE_MESSAGE -> onSimulateMessage()
                            SettingTarget.RESTART_ONBOARDING -> if (!onRestartOnboarding()) scope.launch { toast.show(mockOnly, ToastKind.Info) }
                            SettingTarget.SUPPORT -> openLink("mailto:solaria.innetwork@gmail.com", true)
                            SettingTarget.PRIVACY -> openLink("https://solarianetwork.site/pt-BR/politicas/privacidade")
                            SettingTarget.TERMS -> openLink("https://solarianetwork.site/pt-BR/politicas/termos")
                        }
                    }, subtitle = if (action.target == SettingTarget.LANGUAGE) language.nativeName ?: stringResource(R.string.language_system) else null) }
                }
            }
            Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.onSurfaceMuted)
        }
        AppToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(Spacing.md))
    }
    if (choosingLanguage) BottomSheet(onDismiss = { choosingLanguage = false }, size = SheetSize.Content) {
        val dismiss = LocalSheetDismiss.current
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.xs, bottom = Spacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(stringResource(R.string.language_sheet_title), color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleMedium)
            AppLanguage.entries.forEach { option ->
                val current = option == language
                ActionRow(
                    option.nativeName ?: stringResource(R.string.language_system),
                    if (current) AppIcons.check else AppIcons.globe,
                    {
                        dismiss()
                        if (!current) {
                            language = option
                            context.findActivity()?.let { AppLocale.apply(it, option) }
                        }
                    },
                    trailing = if (current) stringResource(R.string.language_current) else null,
                    compact = true,
                )
            }
        }
    }
    if (confirmSignOut) AppDialog(
        onDismissRequest = { confirmSignOut = false },
        title = stringResource(R.string.settings_sign_out_title),
        message = stringResource(R.string.settings_sign_out_message),
        confirmLabel = stringResource(R.string.settings_sign_out_confirm),
        onConfirm = { confirmSignOut = false; onSignOut() },
        dismissLabel = stringResource(R.string.settings_sign_out_dismiss),
        destructive = true,
    )
}
