package com.project.solaria_mobile.feature.profile

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.mock.MockModeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

enum class SettingTarget { PROFILE, LANGUAGE, PRIVACY, TERMS, SUPPORT, DIAGNOSTICS, SIGN_OUT, SIMULATE_DEEPLINK, SIMULATE_MESSAGE, RESTART_ONBOARDING }

data class SettingAction(@StringRes val title: Int, val icon: Int, val target: SettingTarget)

/** [development] marca o grupo que só existe no QA e abriga também as chaves do modo mock. */
data class SettingsGroup(@StringRes val title: Int, val actions: List<SettingAction>, val development: Boolean = false)

/** [groups] vem completo; a busca filtra na tela, porque só ela sabe o texto no idioma escolhido. */
data class SettingsUiState(val query: String = "", val groups: List<SettingsGroup> = emptyList(), val mockModeEnabled: Boolean = false, val mockImagesEnabled: Boolean = true, val mockEmptyAccount: Boolean = false)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val mockMode: MockModeStore, private val mockChat: com.project.solaria_mobile.core.mock.MockChatRealtime, private val mockData: com.project.solaria_mobile.core.mock.MockApiData) : ViewModel() {
    private val query = MutableStateFlow("")
    private val groups = listOf(
        SettingsGroup(R.string.settings_group_account, listOf(
            SettingAction(R.string.settings_profile, AppIcons.info, SettingTarget.PROFILE),
            SettingAction(R.string.language_title, AppIcons.globe, SettingTarget.LANGUAGE),
        )),
        SettingsGroup(R.string.settings_group_help, listOf(
            SettingAction(R.string.settings_support, AppIcons.mail, SettingTarget.SUPPORT),
            SettingAction(R.string.settings_privacy, AppIcons.shield, SettingTarget.PRIVACY),
            SettingAction(R.string.settings_terms, AppIcons.link, SettingTarget.TERMS),
        )),
        SettingsGroup(R.string.settings_group_session, listOf(SettingAction(R.string.settings_sign_out, AppIcons.log_out, SettingTarget.SIGN_OUT))),
    ) + if (BuildConfig.ENVIRONMENT == "qa") {
        listOf(SettingsGroup(R.string.settings_group_development, listOf(
            SettingAction(R.string.settings_diagnostics, AppIcons.settings, SettingTarget.DIAGNOSTICS),
            SettingAction(R.string.settings_simulate_deeplink, AppIcons.log_out, SettingTarget.SIMULATE_DEEPLINK),
            SettingAction(R.string.st_simular_mensagem, AppIcons.messages_square, SettingTarget.SIMULATE_MESSAGE),
            SettingAction(R.string.st_refazer_onboarding, AppIcons.refresh, SettingTarget.RESTART_ONBOARDING),
        ), development = true))
    } else emptyList()
    val uiState: StateFlow<SettingsUiState> = combine(query, mockMode.enabled, mockMode.imagesEnabled, mockData.emptyAccount) { text, mockEnabled, imagesEnabled, empty ->
        SettingsUiState(query = text, groups = groups, mockModeEnabled = mockEnabled, mockImagesEnabled = imagesEnabled, mockEmptyAccount = empty)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(groups = groups))

    fun onQueryChange(value: String) { query.value = value }

    /** Faz uma mensagem chegar na conversa de demonstração, com "digitando…" antes; só tem efeito no modo mock. */
    /** Apaga o perfil profissional do mock para o onboarding recomeçar; fora do modo mock não há o que apagar. */
    fun restartOnboarding(): Boolean {
        if (!mockMode.isEnabled()) return false
        mockData.resetTechnicianProfile()
        return true
    }

    fun simulateIncomingMessage() { if (mockMode.isEnabled()) mockChat.simulateIncoming() }

    fun onMockModeChange(enabled: Boolean) {
        viewModelScope.launch { mockMode.setEnabled(enabled) }
    }

    fun onMockEmptyAccountChange(enabled: Boolean) {
        mockData.emptyAccount.value = enabled
        viewModelScope.launch { mockMode.setEmptyAccount(enabled) }
    }

    fun onMockImagesChange(enabled: Boolean) {
        viewModelScope.launch { mockMode.setImagesEnabled(enabled) }
    }
}
