package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.feature.discovery.data.DiscoveryRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MapUiState(val saved: Boolean = false, val savedLocations: List<String> = emptyList())

@HiltViewModel
class MapViewModel @Inject constructor(private val repository: DiscoveryRepository) : ViewModel() {
    val uiState: StateFlow<MapUiState> = combine(repository.locationSaved, repository.savedLocations) { saved, locations ->
        MapUiState(saved, locations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState(repository.locationSaved.value, repository.savedLocations.value))

    fun toggleSaved() = repository.toggleLocationSaved()
}
