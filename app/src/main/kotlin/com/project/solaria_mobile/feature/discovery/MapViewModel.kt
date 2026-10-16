package com.project.solaria_mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.common.UiState
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MapUiState(val saved: Boolean = false, val savedLocations: List<String> = emptyList())

@HiltViewModel
class MapViewModel @Inject constructor(private val repository: DiscoveryRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<MapUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<MapUiState>> = mutableState

    init { load() }

    fun load() {
        viewModelScope.launch {
            mutableState.value = UiState.Loading
            val saved = repository.isLocationSaved()
            val locations = repository.savedLocations()
            mutableState.value = when {
                saved is ApiResult.Failure -> UiState.Error(saved.kind)
                locations is ApiResult.Failure -> UiState.Error(locations.kind)
                saved is ApiResult.Success && locations is ApiResult.Success ->
                    UiState.Content(MapUiState(saved.data, locations.data))
                else -> UiState.Error(ErrorKind.UNKNOWN)
            }
        }
    }

    fun toggleSaved() {
        viewModelScope.launch {
            val result = repository.toggleLocationSaved()
            val current = mutableState.value
            if (result is ApiResult.Success && current is UiState.Content) {
                mutableState.value = UiState.Content(current.data.copy(saved = result.data))
            }
        }
    }
}
