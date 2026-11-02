package com.project.solaria_mobile.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.core.common.load
import com.project.solaria_mobile.core.installation.InstallationIdProvider
import com.project.solaria_mobile.feature.discovery.domain.PlaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val environment: String,
    val authUrl: String,
    val coreUrl: String,
    val messengerUrl: String,
    val registryUrl: String,
    val installationId: String?,
    val registryStatus: String,
    val geocodeSample: String,
)

private const val GEOCODE_SAMPLE_QUERY = "Avenida Paulista, São Paulo"

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val installations: InstallationIdProvider,
    private val places: PlaceRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<DiagnosticsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<DiagnosticsUiState>> = mutableState

    init { load() }

    fun load() {
        viewModelScope.launch {
            mutableState.load {
                val installationId = (installations.installationId() as? ApiResult.Success)?.data
                val registryStatus = when (val health = places.isAvailable()) {
                    is ApiResult.Success -> if (health.data) "disponível" else "respondeu com erro"
                    is ApiResult.Failure -> "indisponível (${health.kind.name.lowercase()})"
                }
                val geocodeSample = when (val result = places.geocode(GEOCODE_SAMPLE_QUERY)) {
                    is ApiResult.Success -> result.data.formattedAddress
                    is ApiResult.Failure -> "falhou (${result.kind.name.lowercase()})"
                }
                ApiResult.Success(
                    DiagnosticsUiState(
                        environment = BuildConfig.ENVIRONMENT,
                        authUrl = BuildConfig.AUTH_BASE_URL,
                        coreUrl = BuildConfig.CORE_BASE_URL,
                        messengerUrl = BuildConfig.MESSENGER_BASE_URL,
                        registryUrl = BuildConfig.GOOGLE_REGISTRY_BASE_URL.ifBlank { "desligado" },
                        installationId = installationId,
                        registryStatus = registryStatus,
                        geocodeSample = geocodeSample,
                    )
                )
            }
        }
    }
}
