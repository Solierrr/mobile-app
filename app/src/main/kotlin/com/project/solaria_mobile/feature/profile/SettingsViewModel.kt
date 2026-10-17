package com.project.solaria_mobile.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.composables.icons.lucide.R as LucideR
import com.project.solaria_mobile.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SettingTarget { NONE, PROFILE, DIAGNOSTICS, SIGN_OUT }

data class SettingAction(val title: String, val icon: Int, val target: SettingTarget = SettingTarget.NONE)
data class SettingsGroup(val title: String, val actions: List<SettingAction>)
data class SettingsUiState(val query: String = "", val groups: List<SettingsGroup> = emptyList())

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val query = MutableStateFlow("")
    private val groups = listOf(
        SettingsGroup("Personalização", listOf(SettingAction("Alterar informações", LucideR.drawable.lucide_ic_info, SettingTarget.PROFILE), SettingAction("E-mail", LucideR.drawable.lucide_ic_mail))),
        SettingsGroup("Privacidade", listOf(SettingAction("Marcar como resolvido", LucideR.drawable.lucide_ic_check), SettingAction("Entrar em contato", LucideR.drawable.lucide_ic_message_circle), SettingAction("Denunciar", LucideR.drawable.lucide_ic_shield_alert))),
        SettingsGroup("Integrações", listOf(SettingAction("Google Maps", LucideR.drawable.lucide_ic_map_pin), SettingAction("Google Agenda", LucideR.drawable.lucide_ic_calendar_days))),
        SettingsGroup("Preferências", listOf(SettingAction("Notificações", LucideR.drawable.lucide_ic_bell), SettingAction("Privacidade da conta", LucideR.drawable.lucide_ic_lock), SettingAction("Sair da conta", LucideR.drawable.lucide_ic_log_out, SettingTarget.SIGN_OUT))),
    ) + if (BuildConfig.ENVIRONMENT == "qa") {
        listOf(SettingsGroup("Desenvolvimento", listOf(SettingAction("Diagnóstico", LucideR.drawable.lucide_ic_settings, SettingTarget.DIAGNOSTICS))))
    } else {
        emptyList()
    }
    val uiState: StateFlow<SettingsUiState> = query.map { text ->
        SettingsUiState(text, groups.map { group -> group.copy(actions = group.actions.filter { it.title.contains(text, ignoreCase = true) }) }.filter { it.actions.isNotEmpty() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(groups = groups))

    fun onQueryChange(value: String) { query.value = value }
}
