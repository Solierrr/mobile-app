package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SettingTarget { PROFILE, PRIVACY, TERMS, SUPPORT, DIAGNOSTICS, SIGN_OUT }

data class SettingAction(val title: String, val icon: Int, val target: SettingTarget)
data class SettingsGroup(val title: String, val actions: List<SettingAction>)
data class SettingsUiState(val query: String = "", val groups: List<SettingsGroup> = emptyList())

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val query = MutableStateFlow("")
    private val groups = listOf(
        SettingsGroup("Sua conta", listOf(SettingAction("Meu perfil", AppIcons.info, SettingTarget.PROFILE))),
        SettingsGroup("Ajuda e privacidade", listOf(
            SettingAction("Falar com o suporte", AppIcons.mail, SettingTarget.SUPPORT),
            SettingAction("Política de privacidade", AppIcons.shield, SettingTarget.PRIVACY),
            SettingAction("Termos de uso", AppIcons.link, SettingTarget.TERMS),
        )),
        SettingsGroup("Sessão", listOf(SettingAction("Sair da conta", AppIcons.log_out, SettingTarget.SIGN_OUT))),
    ) + if (BuildConfig.ENVIRONMENT == "qa") {
        listOf(SettingsGroup("Desenvolvimento", listOf(SettingAction("Diagnóstico", AppIcons.settings, SettingTarget.DIAGNOSTICS))))
    } else emptyList()
    val uiState: StateFlow<SettingsUiState> = query.map { text ->
        SettingsUiState(text, groups.map { group -> group.copy(actions = group.actions.filter { it.title.contains(text, ignoreCase = true) }) }.filter { it.actions.isNotEmpty() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(groups = groups))

    fun onQueryChange(value: String) { query.value = value }
}
